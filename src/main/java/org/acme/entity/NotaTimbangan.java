package org.acme.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
public class NotaTimbangan extends PanacheEntity {

    // === Informasi Umum ===
    public String nomorNota;
    public LocalDate tanggalTimbang;
    public LocalTime waktu;
    public String petugasTimbang;
    public String nomorPolisi;
    public String namaCustomer;
    public String kategori;

    // === Data Timbangan ===
    public double beratKotor;
    public double beratTara;
    public double netto1;          // dihitung otomatis jika belum diset
    public double potongan;        // dalam persen
    public double netto2;          // dihitung otomatis jika belum diset

    // === Tanda Tangan ===
    public String tandaTanganCustomer;
    public String tandaTanganPetugas;

    // === Metadata ===
    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        // Auto-set waktu pencatatan
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (this.tanggalTimbang == null) {
            this.tanggalTimbang = LocalDate.now();
        }

        if (this.waktu == null) {
            this.waktu = LocalTime.now();
        }

        // Pastikan nilai negatif tidak masuk
        if (this.beratKotor < 0) this.beratKotor = 0;
        if (this.beratTara < 0) this.beratTara = 0;
        if (this.potongan < 0) this.potongan = 0;

        // Hitung Netto 1 jika belum dihitung
        if (this.netto1 == 0 && this.beratKotor > 0 && this.beratTara >= 0) {
            this.netto1 = this.beratKotor - this.beratTara;
        }

        // Hitung Netto 2 jika belum dihitung
        if (this.netto2 == 0 && this.netto1 > 0 && this.potongan >= 0) {
            this.netto2 = this.netto1 - (this.netto1 * (this.potongan / 100.0));
        }

        // (Opsional) Logging
        System.out.println(">>> [prePersist] Nota disiapkan otomatis sebelum disimpan.");
    }
}
