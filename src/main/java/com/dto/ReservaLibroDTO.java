package com.dto;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaLibroDTO {
    private String id;
    private String userId;
    private String libroId;
    private String fechaReserva;
    private String fechaDevolucion;
    private String status;
}