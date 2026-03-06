package com.ziyan.item.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "items")
public class Item {

    @Id
    private String id;

    private String name;

    private Double price; // Unit price

    private String upc; // Universal Product Code

    private List<String> pictureUrls; // Item picture URLs

    private Integer stock; // Remaining available units

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}