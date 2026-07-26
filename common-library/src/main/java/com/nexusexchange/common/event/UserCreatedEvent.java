package com.nexusexchange.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UserCreatedEvent extends BaseEvent {
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
}
