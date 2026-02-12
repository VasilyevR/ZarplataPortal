package og.portal.zarplata.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import og.portal.zarplata.enums.AppRole;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "role_mapping")
public class RoleMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_role", nullable = false, unique = true)
    private AppRole appRole;

    @Column(name = "ad_group_name", nullable = false)
    private String adGroupName;
}
