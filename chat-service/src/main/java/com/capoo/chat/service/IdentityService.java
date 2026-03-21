package com.capoo.chat.service;

import com.capoo.chat.dto.request.IntrospectRequest;
import com.capoo.chat.dto.response.IntrospectResponse;
import com.capoo.chat.repository.httpclient.IdentityClient;
import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IdentityService {
    IdentityClient identityClient;
    public IntrospectResponse introspectToken(IntrospectRequest request){
        try {
            var result= identityClient.introspectToken(request);
            if (Objects.isNull(result)){
                return IntrospectResponse.builder()
                        .valid(false)
                        .build();
            }
            return result.getResult();
        } catch (FeignException e){
            return IntrospectResponse.builder()
                    .valid(false)
                    .build();
        }

    }
}
