package og.portal.zarplata.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
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

    public User() {
    }
}