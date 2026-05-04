package com.dto;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibroDTO {
    private String id_libro;
    private String titulo;
    private String escritor;
    private String genero;
    private Integer anio;
    private String premisa;
    private Integer stock;
}
