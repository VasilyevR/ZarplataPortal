package og.portal.zarplata.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "login", unique = true, length = 50)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private String login;

    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "user_group_id")
    private UserGroup userGroup;

    @Column(name = "percent", precision = 5, scale = 2)
    private BigDecimal percent;
}