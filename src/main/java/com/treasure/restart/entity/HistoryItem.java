package com.treasure.restart.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryItem {
    private String title;

    private String description;

    private String content;

    private String button;

    private List<String> imageList;
}
