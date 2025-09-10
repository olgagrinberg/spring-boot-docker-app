package com.example.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Entity
@Table(name = "roles", uniqueConstraints = {@UniqueConstraint(columnNames = "name")} )
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Size(max = 60)
    @NotNull
    private RoleName name;


    public enum RoleName {
        USER,
        ADMIN
    }

         /* for future use will be added to  User entity

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "roles",
            joinColumns = @JoinColumn(name = "id"))
    @Column(name = "name")
    private Set<String> roles = new HashSet<>();
*/
}