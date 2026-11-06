package chandanv.local.chandanv.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import chandanv.local.chandanv.models.entity.Admin;
import chandanv.local.chandanv.repositories.AdminRepository;

class AdminServiceTest {

    private final AdminRepository repo = mock(AdminRepository.class);
    private final AdminService service = new AdminService(repo, new BCryptPasswordEncoder(4), 12);

    private Admin admin(String matKhau) {
        Admin a = new Admin();
        a.setUsername("nv");
        a.setMatKhau(matKhau);
        when(repo.findAll()).thenReturn(List.of(a));
        return a;
    }

    @Test
    void chuaCoMatKhau_taoNgauNhienVaBam() {
        Admin a = admin(null);
        assertEquals(1, service.bamMatKhauCu());
        assertTrue(AdminService.laBcrypt(a.getMatKhau()));
        verify(repo).save(a);
    }

    @Test
    void daBamRoi_khongDoi() {
        String hash = new BCryptPasswordEncoder(4).encode("abc");
        Admin a = admin(hash);
        assertEquals(0, service.bamMatKhauCu());
        assertEquals(hash, a.getMatKhau());
        verify(repo, never()).save(a);
    }
}
