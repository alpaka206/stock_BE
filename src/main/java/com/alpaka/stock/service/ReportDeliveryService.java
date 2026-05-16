package com.alpaka.stock.service;

import com.alpaka.stock.api.dto.ReportDtos.ReportDeliveryResponse;
import com.alpaka.stock.api.dto.ReportDtos.ReportPreviewRequest;
import com.alpaka.stock.api.dto.ReportDtos.ReportPreviewResponse;
import com.alpaka.stock.api.dto.ReportDtos.ReportSendRequest;
import com.alpaka.stock.domain.Instrument;
import com.alpaka.stock.domain.PriceBar;
import com.alpaka.stock.domain.ReportDelivery;
import com.alpaka.stock.domain.SourceMaterial;
import com.alpaka.stock.repository.InstrumentRepository;
import com.alpaka.stock.repository.PriceBarRepository;
import com.alpaka.stock.repository.ReportDeliveryRepository;
import com.alpaka.stock.repository.SourceMaterialRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportDeliveryService {
    private final ReportDeliveryRepository reportDeliveryRepository;
    private final SourceMaterialRepository sourceMaterialRepository;
    private final InstrumentRepository instrumentRepository;
    private final PriceBarRepository priceBarRepository;
    private final JavaMailSender mailSender;
    private final boolean emailSendingEnabled;
    private final String fromEmail;

    public ReportDeliveryService(
        ReportDeliveryRepository reportDeliveryRepository,
        SourceMaterialRepository sourceMaterialRepository,
        InstrumentRepository instrumentRepository,
        PriceBarRepository priceBarRepository,
        JavaMailSender mailSender,
        @Value("${stock.reports.email-sending-enabled:false}") boolean emailSendingEnabled,
        @Value("${stock.reports.from-email:no-reply@stock-desk.local}") String fromEmail
    ) {
        this.reportDeliveryRepository = reportDeliveryRepository;
        this.sourceMaterialRepository = sourceMaterialRepository;
        this.instrumentRepository = instrumentRepository;
        this.priceBarRepository = priceBarRepository;
        this.mailSender = mailSender;
        this.emailSendingEnabled = emailSendingEnabled;
        this.fromEmail = fromEmail;
    }

    @Transactional(readOnly = true)
    public ReportPreviewResponse preview(ReportPreviewRequest request) {
        RenderedReport report = render(
            request.userId(),
            request.deliveryEmail(),
            request.locale(),
            request.cadence().name(),
            request.symbols()
        );
        return new ReportPreviewResponse(report.subject(), report.textBody(), report.htmlBody(), OffsetDateTime.now());
    }

    @Transactional
    public ReportDeliveryResponse send(ReportSendRequest request) {
        RenderedReport report = render(
            request.userId(),
            request.deliveryEmail(),
            request.locale(),
            request.cadence().name(),
            request.symbols()
        );
        ReportDelivery delivery = new ReportDelivery(
            null,
            request.userId(),
            request.locale(),
            request.cadence(),
            request.deliveryEmail(),
            report.subject(),
            report.textBody(),
            report.htmlBody()
        );
        reportDeliveryRepository.save(delivery);

        if (!emailSendingEnabled) {
            return toResponse(delivery);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(request.deliveryEmail());
            helper.setSubject(report.subject());
            helper.setText(report.textBody(), report.htmlBody());
            mailSender.send(message);
            delivery.markSent();
        } catch (MailException | MessagingException exc) {
            delivery.markFailed(exc.getMessage());
        }

        return toResponse(delivery);
    }

    @Transactional(readOnly = true)
    public List<ReportDeliveryResponse> deliveries(String userId) {
        return reportDeliveryRepository.findTop50ByUserIdOrderByGeneratedAtDesc(userId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private RenderedReport render(
        String userId,
        String deliveryEmail,
        String locale,
        String cadence,
        List<String> symbols
    ) {
        List<String> normalizedSymbols = symbols == null
            ? List.of()
            : symbols.stream()
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .map(symbol -> symbol.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .limit(12)
                .toList();
        List<InstrumentLine> instrumentLines = normalizedSymbols.stream()
            .map(this::instrumentLine)
            .toList();
        List<SourceMaterial> materials = sourceMaterialRepository.findTop20ByOrderByPublishedAtDesc();
        String generatedAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        boolean korean = locale == null || locale.toLowerCase(Locale.ROOT).startsWith("ko");
        String cadenceLabel = "DAILY".equalsIgnoreCase(cadence)
            ? (korean ? "오늘" : "Today")
            : (korean ? "이번 주" : "This week");
        String subject = korean
            ? "[Stock Desk] %s 주식 리포트".formatted(cadenceLabel)
            : "[Stock Desk] %s stock report".formatted(cadenceLabel);

        List<String> lines = new ArrayList<>();
        lines.add(korean ? "요약" : "Summary");
        lines.add(korean
            ? "서버에 저장된 가격, 뉴스, 공시를 기준으로 작성한 리포트입니다."
            : "This report is generated from price, news, and disclosure records stored on the backend.");
        lines.add("generatedAt=" + generatedAt);
        lines.add("userId=" + userId);
        lines.add("deliveryEmail=" + deliveryEmail);
        lines.add("");
        lines.add(korean ? "관심 종목" : "Tracked symbols");
        if (instrumentLines.isEmpty()) {
            lines.add(korean ? "- 아직 선택한 종목이 없습니다." : "- No symbols selected yet.");
        } else {
            instrumentLines.forEach(line -> lines.add("- " + line.text()));
        }
        lines.add("");
        lines.add(korean ? "최근 자료" : "Recent materials");
        if (materials.isEmpty()) {
            lines.add(korean ? "- 저장된 뉴스나 공시가 아직 없습니다." : "- No stored news or disclosures yet.");
        } else {
            materials.stream()
                .limit(8)
                .forEach(material -> lines.add("- [%s] %s / %s".formatted(
                    material.getProvider(),
                    material.getTitle(),
                    material.getPublishedAt() == null ? "날짜 없음" : material.getPublishedAt().toLocalDate().toString()
                )));
        }

        String textBody = String.join("\n", lines);
        String htmlBody = "<html><body style=\"font-family:system-ui,sans-serif;line-height:1.55;color:#171717;\">"
            + "<h2 style=\"margin:0 0 12px;\">%s</h2>".formatted(escape(subject))
            + "<p>서버에 저장된 가격, 뉴스, 공시를 기준으로 작성한 리포트입니다.</p>"
            + "<h3>관심 종목</h3><ul>"
            + (instrumentLines.isEmpty()
                ? "<li>아직 선택한 종목이 없습니다.</li>"
                : instrumentLines.stream().map(line -> "<li>" + escape(line.text()) + "</li>").reduce("", String::concat))
            + "</ul><h3>최근 자료</h3><ul>"
            + (materials.isEmpty()
                ? "<li>저장된 뉴스나 공시가 아직 없습니다.</li>"
                : materials.stream().limit(8).map(material ->
                    "<li><strong>%s</strong> %s <span style=\"color:#737373;\">%s</span></li>".formatted(
                        escape(material.getProvider()),
                        escape(material.getTitle()),
                        material.getPublishedAt() == null ? "" : material.getPublishedAt().toLocalDate()
                    )
                ).reduce("", String::concat))
            + "</ul></body></html>";

        return new RenderedReport(subject, textBody, htmlBody);
    }

    private InstrumentLine instrumentLine(String symbol) {
        return instrumentRepository.findFirstBySymbolIgnoreCase(symbol)
            .map(instrument -> {
                List<PriceBar> bars = priceBarRepository.findTop2ByInstrumentOrderByTradeDateDesc(instrument);
                if (bars.isEmpty()) {
                    return new InstrumentLine("%s %s: 저장된 가격 없음".formatted(instrument.getSymbol(), instrument.getName()));
                }
                PriceBar latest = bars.get(0);
                String change = bars.size() < 2
                    ? "비교 가격 없음"
                    : latest.getClosePrice().subtract(bars.get(1).getClosePrice()).toPlainString();
                return new InstrumentLine("%s %s: 종가 %s, 전일 대비 %s".formatted(
                    instrument.getSymbol(),
                    instrument.getName(),
                    latest.getClosePrice().toPlainString(),
                    change
                ));
            })
            .orElseGet(() -> new InstrumentLine(symbol + ": 서버에 등록되지 않은 종목"));
    }

    private ReportDeliveryResponse toResponse(ReportDelivery delivery) {
        return new ReportDeliveryResponse(
            delivery.getId(),
            delivery.getUserId(),
            delivery.getDeliveryEmail(),
            delivery.getLocale(),
            delivery.getCadence(),
            delivery.getSubject(),
            delivery.getStatus(),
            delivery.getErrorMessage(),
            delivery.getGeneratedAt(),
            delivery.getSentAt()
        );
    }

    private String escape(String value) {
        return value == null ? "" : value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private record RenderedReport(String subject, String textBody, String htmlBody) {
    }

    private record InstrumentLine(String text) {
    }
}
