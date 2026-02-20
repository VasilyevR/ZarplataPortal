package og.portal.zarplata.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "supplier_setting")
public class SupplierSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "color_name")
    private String colorName;

    @Column(name = "color_hex", unique = true)
    private String colorHex;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "is_default")
    private Boolean isDefault;
}
