package com.laptophub.security;

import com.laptophub.user.User;
import com.laptophub.user.UserRole;
import com.laptophub.user.UserService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private AccessTokenService accessTokenService;

    @Mock
    private UserService userService;

    // Không dùng field initializer: với MockitoExtension, field initializer
    // chạy TRƯỚC khi @Mock được inject, nên filter sẽ giữ tham chiếu null.
    // Phải khởi tạo trong @BeforeEach, chạy SAU khi mock đã sẵn sàng.
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(accessTokenService, userService);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private User activeUser() {
        User user = User.create("filter@example.com", "hash", "Name", null, UserRole.CUSTOMER);
        user.setId(7L);
        return user;
    }

    @Test
    void noAuthorizationHeader_doesNotAuthenticate_andContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/anything");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void malformedAuthorizationHeader_doesNotAuthenticate_andContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/anything");
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void validTokenForActiveUser_setsAuthenticationWithCorrectAuthority() throws Exception {
        User user = activeUser();
        when(accessTokenService.parse("good-token"))
                .thenReturn(new AccessTokenClaims(7L, "filter@example.com", UserRole.CUSTOMER, "jti-1"));
        when(userService.findById(7L)).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/anything");
        request.addHeader("Authorization", "Bearer good-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(UserPrincipal.class);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CUSTOMER");
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidToken_doesNotAuthenticate_butStillContinuesChain() throws Exception {
        when(accessTokenService.parse("bad-token")).thenThrow(new InvalidAccessTokenException("invalid", null));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/anything");
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenForNonExistentUser_doesNotAuthenticate() throws Exception {
        when(accessTokenService.parse("token-of-deleted-user"))
                .thenReturn(new AccessTokenClaims(99L, "gone@example.com", UserRole.CUSTOMER, "jti-2"));
        when(userService.findById(99L)).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/anything");
        request.addHeader("Authorization", "Bearer token-of-deleted-user");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenForBlockedUser_doesNotAuthenticate_evenThoughTokenItselfIsValid() throws Exception {
        User user = activeUser();
        user.block();
        when(accessTokenService.parse("token-of-blocked-user"))
                .thenReturn(new AccessTokenClaims(7L, "filter@example.com", UserRole.CUSTOMER, "jti-3"));
        when(userService.findById(7L)).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/anything");
        request.addHeader("Authorization", "Bearer token-of-blocked-user");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}
