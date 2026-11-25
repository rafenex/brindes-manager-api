package br.com.rafael.brindesmanager;

import br.com.rafael.brindesmanager.dto.response.AuthResponse;
import br.com.rafael.brindesmanager.dto.response.UserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserManagementIntegrationTest {

    private static final String PASSWORD = "test-password";
    private static final String USERS = "/api/users";

    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;

    private final List<Long> companies = new ArrayList<>();

    @AfterEach
    void removeFixtures() {
        for (Long company : companies) {
            jdbc.update("delete from customer_orders where company_id = ?", company);
            jdbc.update("delete from customers where company_id = ?", company);
            jdbc.update("delete from app_users where company_id = ?", company);
            jdbc.update("delete from companies where id = ?", company);
        }
    }

    @Test
    void editsWithCurrentIdentityAndRejectsInvalidOrUnauthorizedChanges() {
        AuthResponse admin = register();
        AuthResponse foreign = register();
        UserResponse user = create(admin);
        String userToken = login(user.email()).getBody().token();
        UserResponse peer = create(admin);
        jdbc.update("update app_users set role = 'ADMIN' where id = ?", peer.id());

        var changes = Map.of("name", "Atualizado", "email", email());
        for (HttpMethod method : List.of(HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE)) {
            Object body = method == HttpMethod.PUT ? changes : null;
            assertThat(call(method, user.id(), null, body).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(call(method, peer.id(), userToken, body).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(call(method, admin.userId(), admin.token(), body).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(call(method, foreign.userId(), admin.token(), body).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
        assertThat(call(HttpMethod.GET, user.id(), admin.token(), null).getBody()).contains(user.email());
        assertThat(call(HttpMethod.PUT, user.id(), admin.token(), Map.of("name", "", "email", user.email())).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.PUT, user.id(), admin.token(), Map.of("name", "Nome", "email", "invalido")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.PUT, user.id(), admin.token(), Map.of("name", "Nome", "email", peer.email().toUpperCase())).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, user.id(), admin.token(), null).getBody()).contains(user.name(), user.email());

        assertThat(call(HttpMethod.PUT, user.id(), admin.token(), Map.of("name", "Mesmo e-mail", "email", user.email())).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.PUT, peer.id(), admin.token(), changes).getBody()).contains("Atualizado", "ADMIN");
        String newEmail = email();
        assertThat(call(HttpMethod.PUT, user.id(), admin.token(), Map.of("name", "Nome novo", "email", newEmail)).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.GET, null, admin.token(), null).getBody()).contains("Nome novo", newEmail);
        assertThat(login(newEmail).getBody().userId()).isEqualTo(user.id());

        jdbc.update("update app_users set email = ?, role = 'ADMIN' where id = ?", user.email(), peer.id());
        assertThat(call(HttpMethod.GET, null, userToken, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        String peerToken = login(user.email()).getBody().token();
        jdbc.update("update app_users set role = 'USER' where id = ?", peer.id());
        assertThat(call(HttpMethod.PUT, user.id(), peerToken, changes).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.GET, user.id(), admin.token(), null).getBody()).contains(newEmail);
    }

    @Test
    void removesAccountFromManagementAndRejectsItsExistingSessionWhilePreservingHistory() {
        AuthResponse admin = register();
        UserResponse user = create(admin);
        jdbc.update("update app_users set role = 'ADMIN' where id = ?", user.id());
        String priorToken = login(user.email()).getBody().token();
        Long company = companies.getFirst();
        jdbc.update("""
                insert into customers(company_name, active, user_id, company_id, created_at)
                values ('Histórico', true, ?, ?, now())
                """, user.id(), company);
        jdbc.update("""
                insert into customer_orders(code, customer_id, user_id, status, total_amount, active, created_at, company_id)
                select ?, id, user_id, 'PENDING', 0, true, now(), company_id from customers where user_id = ?
                """, "TEST-" + user.id(), user.id());
        assertThat(call(HttpMethod.GET, null, priorToken, null).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(call(HttpMethod.DELETE, user.id(), admin.token(), null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(call(HttpMethod.GET, null, admin.token(), null).getBody()).doesNotContain(user.email());
        assertThat(login(user.email()).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(call(HttpMethod.GET, null, priorToken, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(call(HttpMethod.DELETE, user.id(), admin.token(), null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.PUT, user.id(), admin.token(), Map.of("name", "Alterado", "email", email())).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(jdbc.queryForObject("select count(*) from customers where user_id = ?", Long.class, user.id())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select count(*) from customer_orders where user_id = ?", Long.class, user.id())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select active from app_users where id = ?", Boolean.class, user.id())).isFalse();
    }

    @Test
    void concurrentEditsCannotClaimTheSameEmailIgnoringCase() throws Exception {
        AuthResponse firstAdmin = register();
        AuthResponse secondAdmin = register();
        UserResponse first = create(firstAdmin);
        UserResponse second = create(secondAdmin);
        String shared = email();
        var left = CompletableFuture.supplyAsync(() -> call(HttpMethod.PUT, first.id(), firstAdmin.token(),
                Map.of("name", "Primeiro", "email", shared)).getStatusCode());
        var right = CompletableFuture.supplyAsync(() -> call(HttpMethod.PUT, second.id(), secondAdmin.token(),
                Map.of("name", "Segundo", "email", shared.toUpperCase())).getStatusCode());
        assertThat(List.of(left.get(10, TimeUnit.SECONDS), right.get(10, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.BAD_REQUEST);
    }

    @Test
    void unicodeEmailAliasesShareAnIndexKeyAndResolveToOneLogin() throws Exception {
        AuthResponse firstAdmin = register();
        AuthResponse secondAdmin = register();
        UserResponse first = create(firstAdmin);
        UserResponse second = create(secondAdmin);
        String suffix = email();
        // PostgreSQL uppercases both forms of the Greek lowercase sigma to the same character.
        String normalSigma = "\u03c3" + suffix;
        String finalSigma = "\u03c2" + suffix;

        var firstEdit = CompletableFuture.supplyAsync(() -> call(HttpMethod.PUT, first.id(), firstAdmin.token(),
                Map.of("name", "Primeiro", "email", normalSigma)).getStatusCode());
        var secondEdit = CompletableFuture.supplyAsync(() -> call(HttpMethod.PUT, second.id(), secondAdmin.token(),
                Map.of("name", "Segundo", "email", finalSigma)).getStatusCode());
        var firstStatus = firstEdit.get(10, TimeUnit.SECONDS);
        var secondStatus = secondEdit.get(10, TimeUnit.SECONDS);
        assertThat(List.of(firstStatus, secondStatus)).containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.BAD_REQUEST);

        long winner = firstStatus == HttpStatus.OK ? first.id() : second.id();
        long loser = winner == first.id() ? second.id() : first.id();
        String alternate = winner == first.id() ? finalSigma : normalSigma;
        assertThatThrownBy(() -> jdbc.update("update app_users set email = ? where id = ?", alternate, loser))
                .isInstanceOf(DuplicateKeyException.class);

        assertThat(login(normalSigma).getBody().userId()).isEqualTo(winner);
        assertThat(login(finalSigma).getBody().userId()).isEqualTo(winner);
        assertThat(http.exchange(USERS, HttpMethod.POST, request(firstAdmin.token(),
                Map.of("name", "Duplicado", "email", alternate, "password", PASSWORD)), String.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(http.postForEntity("/api/auth/register",
                Map.of("name", "Duplicado", "companyName", "Duplicada", "email", alternate, "password", PASSWORD),
                String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private AuthResponse register() {
        var result = http.postForEntity("/api/auth/register",
                Map.of("name", "Admin de teste", "companyName", "Empresa " + UUID.randomUUID(),
                        "email", email(), "password", PASSWORD), AuthResponse.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AuthResponse admin = result.getBody();
        companies.add(jdbc.queryForObject("select company_id from app_users where id = ?", Long.class, admin.userId()));
        return admin;
    }

    private UserResponse create(AuthResponse admin) {
        var result = http.exchange(USERS, HttpMethod.POST, request(admin.token(),
                Map.of("name", "Conta de teste", "email", email(), "password", PASSWORD)), UserResponse.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return result.getBody();
    }

    private ResponseEntity<AuthResponse> login(String email) {
        return http.postForEntity("/api/auth/login", Map.of("email", email, "password", PASSWORD), AuthResponse.class);
    }

    private ResponseEntity<String> call(HttpMethod method, Long id, String token, Object body) {
        return http.exchange(id == null ? USERS : USERS + "/" + id, method, request(token, body), String.class);
    }

    private HttpEntity<Object> request(String token, Object body) {
        var headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return new HttpEntity<>(body, headers);
    }

    private String email() {
        return UUID.randomUUID() + "@example.test";
    }
}
