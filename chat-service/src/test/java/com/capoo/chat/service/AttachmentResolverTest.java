package com.capoo.chat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.capoo.chat.entity.Attachment;
import com.capoo.chat.exception.AppException;
import com.capoo.chat.exception.ErrorCode;
import com.capoo.chat.repository.httpclient.StorageClient;
import com.capoo.chat.repository.httpclient.StorageClient.Response;
import com.capoo.chat.repository.httpclient.StorageClient.StoredFile;

import feign.FeignException;
import feign.Request;

class AttachmentResolverTest {
    private static final String PUBLIC = "http://gw/api/v1/storage-service/files";

    private StorageClient storageClient;
    private AttachmentResolver resolver;

    @BeforeEach
    void setUp() {
        storageClient = mock(StorageClient.class);
        resolver = new AttachmentResolver(storageClient, PUBLIC);
    }

    private void givenFile(String id, String mimeType, String extension, String name, long size) {
        when(storageClient.getFile(id))
                .thenReturn(new Response<>(true, new StoredFile(id, extension, name, mimeType, size), ""));
    }

    private AppException rejected(List<String> ids) {
        return assertThrows(AppException.class, () -> resolver.resolve(ids));
    }

    /* ---------------------------------------------------- happy path ---------------------------------------------------- */

    @Test
    void image_becomesAnImageAttachment_withWebpAndThumbnailLinks() {
        givenFile("img1", "image/png", ".png", "cat.png", 1234);

        Attachment a = resolver.resolve(List.of("img1")).get(0);

        assertEquals("img1", a.getFileId());
        assertEquals(Attachment.IMAGE, a.getType());
        assertEquals(PUBLIC + "/web/img1.webp", a.getUrl());
        assertEquals(PUBLIC + "/thumbnail/img1.webp", a.getThumbnailUrl());
        assertEquals("cat.png", a.getName());
        assertEquals(1234L, a.getSize());
    }

    @Test
    void video_becomesAVideoAttachment_streamedWithItsExtension() {
        givenFile("vid1", "video/mp4", ".mp4", "clip.mp4", 5_000_000);

        Attachment a = resolver.resolve(List.of("vid1")).get(0);

        assertEquals(Attachment.VIDEO, a.getType());
        assertEquals(PUBLIC + "/stream-video/vid1.mp4", a.getUrl());
        // storage cuts a poster frame from videos too, served by the same thumbnail endpoint as image thumbnails
        assertEquals(PUBLIC + "/thumbnail/vid1.webp", a.getThumbnailUrl());
    }

    @Test
    void extensionWithoutDot_isNormalised() {
        givenFile("vid1", "video/webm", "webm", "clip.webm", 10);

        assertEquals(
                PUBLIC + "/stream-video/vid1.webm",
                resolver.resolve(List.of("vid1")).get(0).getUrl());
    }

    @Test
    void imagesAndVideosMixed_keepTheOrderTheyWereGivenIn() {
        givenFile("a", "image/jpeg", ".jpg", "a.jpg", 1);
        givenFile("b", "video/mp4", ".mp4", "b.mp4", 2);
        givenFile("c", "image/gif", ".gif", "c.gif", 3);

        var result = resolver.resolve(List.of("b", "a", "c"));

        assertEquals(
                List.of("b", "a", "c"),
                result.stream().map(Attachment::getFileId).toList());
        assertEquals(
                List.of("VIDEO", "IMAGE", "IMAGE"),
                result.stream().map(Attachment::getType).toList());
    }

    @Test
    void exactlyFiveIsAllowed() {
        for (String id : List.of("1", "2", "3", "4", "5")) {
            givenFile(id, "image/png", ".png", id + ".png", 1);
        }

        assertEquals(5, resolver.resolve(List.of("1", "2", "3", "4", "5")).size());
    }

    @Test
    void noFiles_isFine_andGivesAnEmptyList() {
        assertTrue(resolver.resolve(null).isEmpty());
        assertTrue(resolver.resolve(List.of()).isEmpty());
        verifyNoInteractions(storageClient);
    }

    @Test
    void repeatedAndBlankIds_areIgnored_andLookedUpOnce() {
        givenFile("a", "image/png", ".png", "a.png", 1);

        var result = resolver.resolve(Arrays.asList("a", " a ", "a", "", "  ", null));

        assertEquals(1, result.size());
        verify(storageClient, times(1)).getFile("a");
    }

    /* ---------------------------------------------------- rejections ---------------------------------------------------- */

    @Test
    void moreThanFive_rejectedBeforeAnythingIsLookedUp() {
        var exception = rejected(List.of("1", "2", "3", "4", "5", "6"));

        assertEquals(ErrorCode.TOO_MANY_ATTACHMENTS, exception.getErrorCode());
        verifyNoInteractions(storageClient);
    }

    @Test
    void repeatedIdsDoNotCountTowardsTheLimit() {
        for (String id : List.of("1", "2", "3", "4", "5")) {
            givenFile(id, "image/png", ".png", id, 1);
        }

        assertEquals(
                5,
                resolver.resolve(List.of("1", "1", "2", "3", "3", "4", "5", "5"))
                        .size());
    }

    @Test
    void audioAndDocuments_areRejected() {
        givenFile("song", "audio/mpeg", ".mp3", "song.mp3", 1);
        givenFile("doc", "application/pdf", ".pdf", "doc.pdf", 1);

        assertEquals(
                ErrorCode.UNSUPPORTED_ATTACHMENT_TYPE, rejected(List.of("song")).getErrorCode());
        assertEquals(
                ErrorCode.UNSUPPORTED_ATTACHMENT_TYPE, rejected(List.of("doc")).getErrorCode());
    }

    @Test
    void storageAnswering500ForAnUnknownId_isTreatedAsNotFound() {
        // what storage-service really does for an id it does not know
        Request request = Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, StandardCharsets.UTF_8, null);
        when(storageClient.getFile("ghost"))
                .thenThrow(new FeignException.InternalServerError("boom", request, null, null));

        assertEquals(ErrorCode.ATTACHMENT_NOT_FOUND, rejected(List.of("ghost")).getErrorCode());
    }

    @Test
    void storageUnreachable_isNotReportedAsABadId() {
        Request request = Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, StandardCharsets.UTF_8, null);
        // status -1: no HTTP answer at all (connection refused, timeout)
        when(storageClient.getFile("any"))
                .thenThrow(new FeignException.ServiceUnavailable("down", request, null, null));

        var exception = assertThrows(FeignException.class, () -> resolver.resolve(List.of("any")));

        assertEquals(503, exception.status());
    }

    @Test
    void fileWithoutAType_isRejected() {
        when(storageClient.getFile("odd"))
                .thenReturn(new Response<>(true, new StoredFile("odd", ".x", "odd", null, 1L), ""));

        assertEquals(
                ErrorCode.UNSUPPORTED_ATTACHMENT_TYPE, rejected(List.of("odd")).getErrorCode());
    }

    @Test
    void storageAnswering200WithSuccessFalse_isTreatedAsNotFound() {
        // this is how storage-service reports a missing file
        when(storageClient.getFile("ghost")).thenReturn(new Response<>(false, null, "FILE_NOT_FOUND"));

        assertEquals(ErrorCode.ATTACHMENT_NOT_FOUND, rejected(List.of("ghost")).getErrorCode());
    }

    @Test
    void storageAnswering404_isTreatedAsNotFound() {
        Request request = Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, StandardCharsets.UTF_8, null);
        when(storageClient.getFile("ghost")).thenThrow(new FeignException.NotFound("nope", request, null, null));

        assertEquals(ErrorCode.ATTACHMENT_NOT_FOUND, rejected(List.of("ghost")).getErrorCode());
    }

    @Test
    void oneBadFile_rejectsTheWholeMessage() {
        givenFile("good", "image/png", ".png", "good.png", 1);
        when(storageClient.getFile("bad")).thenReturn(new Response<>(false, null, "FILE_NOT_FOUND"));

        assertEquals(
                ErrorCode.ATTACHMENT_NOT_FOUND, rejected(List.of("good", "bad")).getErrorCode());
    }

    @Test
    void idsThatCouldChangeTheStoragePath_areRefusedWithoutCallingStorage() {
        for (String evil : List.of("../secret", "a/b", "a\\b", "x?y=1", "x#y", "a%2Fb", "..")) {
            assertEquals(ErrorCode.ATTACHMENT_NOT_FOUND, rejected(List.of(evil)).getErrorCode(), evil);
        }
        assertEquals(
                ErrorCode.ATTACHMENT_NOT_FOUND,
                rejected(List.of("x".repeat(201))).getErrorCode());
        verifyNoInteractions(storageClient);
    }

    @Test
    void typeAndLinksComeFromStorage_notFromTheClient() {
        // the client can only name a file, a pdf cannot be passed off as an image
        givenFile("sneaky.png", "application/pdf", ".png", "sneaky.png", 1);

        assertEquals(
                ErrorCode.UNSUPPORTED_ATTACHMENT_TYPE,
                rejected(List.of("sneaky.png")).getErrorCode());
    }
}
