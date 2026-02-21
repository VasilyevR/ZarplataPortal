package og.portal.zarplata.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "color_mapping")
public class ColorMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "excel_argb_hex", unique = true, nullable = false)
    private String excelArgbHex;

    @Column(name = "html_color_code", nullable = false)
    private String htmlColorCode;

    @Column(name = "description")
    private String description;
}