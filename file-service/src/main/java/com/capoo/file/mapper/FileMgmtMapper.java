package com.capoo.file.mapper;

import com.capoo.file.dto.FileInfo;
import com.capoo.file.entity.FileMgmt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FileMgmtMapper {
    @Mapping(target = "id",source = "name")
    FileMgmt toFileMgmt(FileInfo fileInfo);

}
