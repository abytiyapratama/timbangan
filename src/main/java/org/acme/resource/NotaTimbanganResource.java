package org.acme.resource;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.entity.NotaTimbangan;
import io.quarkus.panache.common.Sort;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Path("/nota")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotaTimbanganResource {

    // ✅ Simpan nota timbangan baru
    @POST
    @Transactional
    public NotaTimbangan createNota(NotaTimbangan nota) {
        // Set otomatis jika tidak diisi
        if (nota.tanggalTimbang == null) nota.tanggalTimbang = LocalDate.now();
        if (nota.waktu == null) nota.waktu = LocalTime.now();
        if (nota.petugasTimbang == null) nota.petugasTimbang = "Hendra Bhakti";
        if (nota.tandaTanganPetugas == null) nota.tandaTanganPetugas = nota.petugasTimbang;

        // Hitung netto
        nota.netto1 = nota.beratKotor - nota.beratTara;
        nota.netto2 = nota.netto1 - (nota.netto1 * (nota.potongan / 100.0));

        // Generate nomor nota unik
        long countToday = NotaTimbangan.find("tanggalTimbang = ?1", nota.tanggalTimbang).count();
        String nomorUrut = String.format("%04d", countToday + 1);
        nota.nomorNota = "NOTA-" + nota.tanggalTimbang.toString().replace("-", "") + "-" + nomorUrut;

        nota.persist();
        return nota;
    }

    // ✅ Ambil semua nota
    @GET
    public List<NotaTimbangan> getAll() {
        return NotaTimbangan.listAll(Sort.descending("createdAt"));
    }

    // ✅ Ambil nota 1 bulan terakhir
    @GET
    @Path("/recent")
    public List<NotaTimbangan> getRecent() {
        LocalDateTime satuBulanLalu = LocalDateTime.now().minusMonths(1);
        return NotaTimbangan.find("createdAt >= ?1 ORDER BY createdAt DESC", satuBulanLalu).list();
    }

    // ✅ Export PDF berdasarkan ID nota
    @GET
    @Path("/pdf/{id}")
    @Produces("application/pdf")
    public Response generateNotaPdf(@PathParam("id") Long id) {
        NotaTimbangan nota = NotaTimbangan.findById(id);
        if (nota == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = new Font(Font.FontFamily.COURIER, 14, Font.BOLD);
            Font sectionFont = new Font(Font.FontFamily.COURIER, 12, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.COURIER, 12);

            doc.add(new Paragraph("                       CV. MUTIARA KIKIM", titleFont));
            doc.add(new Paragraph("             JLN. LINTAS SUMATERA, DESA BUNGAMAS", titleFont));
            doc.add(new Paragraph("                             SLIP PENIMBANGAN", sectionFont));
            doc.add(new Paragraph("======================================================================"));

            doc.add(new Paragraph(String.format("   No.Slip           : %s", nota.nomorNota), normalFont));
            doc.add(new Paragraph(String.format("   No.Polisi         : %s", nota.nomorPolisi), normalFont));
            doc.add(new Paragraph("   Kode Relasi       : 60", normalFont));
            doc.add(new Paragraph("   Nama Relasi       : PRIBADI", normalFont));
            doc.add(new Paragraph("   Nama Barang       : TBS", normalFont));
            doc.add(new Paragraph("   Jlh. Tandan       : 0 / Tandan", normalFont));
            doc.add(new Paragraph("                       0,00 Kg/ Tandan", normalFont));
            doc.add(new Paragraph("   Keterangan        : -", normalFont));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph(String.format("   Jam               : %s", nota.waktu.toString().substring(0, 5)), normalFont));
            doc.add(new Paragraph(String.format("   Tgl               : %s", nota.tanggalTimbang), normalFont));
            doc.add(new Paragraph(String.format("   Bruto             : %.0f Kg", nota.beratKotor), normalFont));
            doc.add(new Paragraph(String.format("   Tara              : %.0f Kg", nota.beratTara), normalFont));
            doc.add(new Paragraph(String.format("   Netto             : %.0f Kg", nota.netto1), normalFont));
            doc.add(new Paragraph(String.format("   Potongan          : %.0f Kg", nota.netto1 - nota.netto2), normalFont));
            doc.add(new Paragraph(String.format("   Berat Bersih      : %.0f Kg", nota.netto2), normalFont));

            doc.add(new Paragraph("======================================================================"));
            doc.add(new Paragraph("         Ditimbang,                            Diketahui,", normalFont));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(String.format("       (%s)                    (%s)", nota.petugasTimbang, nota.tandaTanganCustomer), normalFont));

            doc.close();

            return Response.ok(out.toByteArray())
                    .header("Content-Disposition", "inline; filename=nota.pdf")
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }
}
