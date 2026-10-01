package com.treasure.restart.dto;

import lombok.Data;

import java.util.List;

@Data
public class MomentPublishRequest {

    private String content;

    private List<String> images;

    private String location;
}
