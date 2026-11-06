package team.projectpulse.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;
import team.projectpulse.user.UserRepository;

/**
 * Converts a verified JWT only when its account still exists and is enabled.
 *
 * <p>A JWT is self-contained for identity and authorities, but account lifecycle is mutable state. Looking up the
 * account on each protected request enforces FR-SEC-active-account immediately after deactivation rather than waiting
 * for the token's expiry.
 */
@Component
public class CurrentAccountJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;
    private final JwtAuthenticationConverter delegate;

    public CurrentAccountJwtAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;

        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("authorities");
        authoritiesConverter.setAuthorityPrefix("");

        this.delegate = new JwtAuthenticationConverter();
        this.delegate.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Integer userId = userId(jwt);
        boolean enabled = this.userRepository.findById(userId)
                .map(user -> user.isEnabled())
                .orElse(false);
        if (!enabled) {
            throw new InvalidBearerTokenException("The account associated with this access token is disabled or no longer exists.");
        }
        return this.delegate.convert(jwt);
    }

    private Integer userId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (claim instanceof Number number) {
            return number.intValue();
        }
        if (claim instanceof String value) {
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException ignored) {
                // The uniform authentication failure below must not disclose token parsing details.
            }
        }
        throw new InvalidBearerTokenException("The access token does not identify an account.");
    }
}
