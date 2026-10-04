package com.common.storage.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class ResponseModel<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private T data;
    private Boolean success = false;
    private Object error = "";

    public ResponseModel(Boolean success, T data) {
        this.success = success;
        this.data = data;
    }

    public ResponseModel(Boolean success, T data, Object error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }
}
