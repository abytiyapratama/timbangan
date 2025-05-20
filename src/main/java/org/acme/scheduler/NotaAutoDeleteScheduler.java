package org.acme.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.acme.entity.NotaTimbangan;
import java.util.concurrent.TimeUnit;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit; // ✅ import tambahan

@ApplicationScoped
public class NotaAutoDeleteScheduler {

    // Dijalankan setiap 1 jam, dengan delay awal 30 detik setelah startup
    @Scheduled(every = "1h", delay = 30, delayUnit = TimeUnit.SECONDS)
    @Transactional
    public void hapusNotaLama() {
        long count = NotaTimbangan.delete("createdAt < ?1", LocalDateTime.now().minusMonths(1));
        System.out.println(">> Dihapus " + count + " nota lebih dari 1 bulan.");
    }
}
