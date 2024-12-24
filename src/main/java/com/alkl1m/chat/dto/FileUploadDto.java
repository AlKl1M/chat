package com.alkl1m.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUploadDto {
    private String fileContentBase64;
    private String filename;
    private String contentType;
}