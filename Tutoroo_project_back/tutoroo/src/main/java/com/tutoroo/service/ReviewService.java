package com.tutoroo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.tutoroo.entity.*;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.PracticeMapper;
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final PracticeMapper practiceMapper;
    private final StudyMapper studyMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    /**
     * [기능: Day별 맞춤 복습 PDF 생성 (최종판)]
     * 구성:
     * 1. 오늘의 학습 요약 (StudyLog의 dailySummary)
     * 2. 오늘의 실전 오답 노트 (그날 PracticeService에서 틀린 문제 + 해설)
     * 3. 현재 약점 분석 (전체적인 취약점)
     */
    @Transactional(readOnly = true)
    public byte[] generateDailyReviewPdf(Long userId, Long planId, Integer dayCount) {
        UserEntity user = userMapper.findById(userId);
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) throw new TutorooException(ErrorCode.STUDY_PLAN_NOT_FOUND);

        List<StudyLogEntity> logs = studyMapper.findLogsByPlanId(planId);
        StudyLogEntity targetLog = logs.stream()
                .filter(l -> l.getDayCount() != null && l.getDayCount().equals(dayCount))
                .findFirst()
                .orElseThrow(() -> new TutorooException("해당 날짜의 학습 기록이 없습니다.", ErrorCode.INVALID_INPUT_VALUE));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, out);

            doc.open();
            BaseFont baseFont = getKoreanFont();

            // 폰트 스타일
            Font titleFont = new Font(baseFont, 20, Font.BOLD, Color.BLACK);
            Font subTitleFont = new Font(baseFont, 14, Font.BOLD, new Color(80, 80, 80));
            Font bodyFont = new Font(baseFont, 11, Font.NORMAL, Color.BLACK);
            Font questionFont = new Font(baseFont, 11, Font.BOLD, new Color(50, 50, 150)); // 문제: 남색
            Font wrongAnswerFont = new Font(baseFont, 10, Font.NORMAL, new Color(200, 0, 0)); // 오답: 빨강
            Font explanationFont = new Font(baseFont, 10, Font.NORMAL, new Color(0, 100, 0)); // 해설: 초록

            // --- [표지] ---
            Paragraph title = new Paragraph("Day " + dayCount + " 완벽 복습 리포트", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30);
            doc.add(title);

            // 기본 정보
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(20);
            String studyDateStr = targetLog.getStudyDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            addTableRow(infoTable, "학생 이름", user.getName(), bodyFont);
            addTableRow(infoTable, "학습 주제", plan.getGoal(), bodyFont);
            addTableRow(infoTable, "학습 날짜", studyDateStr, bodyFont);
            doc.add(infoTable);

            // --- [Section 1: 오늘의 핵심 요약] ---
            doc.add(new Paragraph("1. 오늘의 핵심 요약 (Daily Summary)", subTitleFont));
            doc.add(new Paragraph(" ", bodyFont));

            String summaryText = (targetLog.getDailySummary() != null && !targetLog.getDailySummary().isBlank())
                    ? targetLog.getDailySummary()
                    : "요약된 내용이 없습니다.";

            Paragraph summaryP = new Paragraph(summaryText, bodyFont);
            summaryP.setIndentationLeft(10);
            summaryP.setSpacingAfter(20);
            doc.add(summaryP);

            // --- [Section 2: 오늘의 실전 오답 노트 (핵심)] ---
            doc.add(new Paragraph("2. 실전 오답 클리닉 (오늘 틀린 문제)", subTitleFont));
            doc.add(new Paragraph(" ", bodyFont));

            List<PracticeMapper.WrongPracticeDetail> wrongDetails =
                    practiceMapper.findWrongLogDetailsByDate(userId, studyDateStr);

            if (wrongDetails != null && !wrongDetails.isEmpty()) {
                int qIndex = 1;
                for (PracticeMapper.WrongPracticeDetail detail : wrongDetails) {
                    // 1. 문제
                    String qText = extractQuestionText(detail.questionJson());
                    doc.add(new Paragraph("Q" + qIndex + ". " + qText, questionFont));

                    // 2. 내가 쓴 오답
                    Paragraph myAns = new Paragraph("❌ 나의 답: " + detail.userAnswer(), wrongAnswerFont);
                    myAns.setIndentationLeft(15);
                    doc.add(myAns);

                    // 3. AI 해설
                    Paragraph aiExp = new Paragraph("💡 해설: " + detail.aiFeedback(), explanationFont);
                    aiExp.setIndentationLeft(15);
                    aiExp.setSpacingAfter(15);
                    doc.add(aiExp);

                    qIndex++;
                }
            } else {
                doc.add(new Paragraph("오늘 실전 연습에서 틀린 문제가 없습니다. 훌륭합니다! 💯", bodyFont));
                doc.add(new Paragraph(" ", bodyFont));
            }

            // --- [Section 3: AI 선생님 코멘트 & 약점 분석] ---
            doc.add(new Paragraph("3. AI 선생님 총평 & 약점 분석", subTitleFont));
            doc.add(new Paragraph(" ", bodyFont));

            // AI 총평
            if (targetLog.getAiFeedback() != null) {
                doc.add(new Paragraph("👨‍🏫 선생님 코멘트: " + targetLog.getAiFeedback(), bodyFont));
                doc.add(new Paragraph(" ", bodyFont));
            }

            // 약점 태그
            List<String> weakTopics = practiceMapper.findTopWeakTopics(userId, planId);
            if (weakTopics != null && !weakTopics.isEmpty()) {
                doc.add(new Paragraph("📊 현재 집중적으로 보완해야 할 토픽:", bodyFont));
                com.lowagie.text.List list = new com.lowagie.text.List(com.lowagie.text.List.UNORDERED);
                list.setIndentationLeft(20);
                for (String topic : weakTopics) {
                    list.add(new ListItem(topic, bodyFont));
                }
                doc.add(list);
            }

            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("PDF 생성 실패", e);
            throw new TutorooException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // --- Helper Methods ---

    private void addTableRow(PdfPTable table, String key, String value, Font font) {
        PdfPCell cellKey = new PdfPCell(new Phrase(key, font));
        cellKey.setBackgroundColor(new Color(240, 240, 240));
        cellKey.setPadding(8);
        cellKey.setBorder(Rectangle.NO_BORDER);

        PdfPCell cellValue = new PdfPCell(new Phrase(value != null ? value : "-", font));
        cellValue.setPadding(8);
        cellValue.setBorder(Rectangle.BOTTOM);
        cellValue.setBorderWidthBottom(1f);
        cellValue.setBorderColorBottom(Color.LIGHT_GRAY);

        table.addCell(cellKey);
        table.addCell(cellValue);
    }

    private String extractQuestionText(String json) {
        try {
            Map map = objectMapper.readValue(json, Map.class);
            return (String) map.getOrDefault("question", "문제 내용 없음");
        } catch (Exception e) {
            return "문제 로딩 실패";
        }
    }

    private BaseFont getKoreanFont() throws IOException, DocumentException {
        String os = System.getProperty("os.name").toLowerCase();
        String fontPath = null;
        if (os.contains("win")) fontPath = "C:/Windows/Fonts/malgun.ttf";
        else if (os.contains("mac")) {
            if (new File("/Library/Fonts/AppleGothic.ttf").exists()) fontPath = "/Library/Fonts/AppleGothic.ttf";
            else if (new File("/System/Library/Fonts/AppleSDGothicNeo.ttc").exists()) fontPath = "/System/Library/Fonts/AppleSDGothicNeo.ttc";
        } else fontPath = "/usr/share/fonts/truetype/nanum/NanumGothic.ttf";

        try {
            if (fontPath != null && new File(fontPath).exists()) return BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            return BaseFont.createFont("fonts/NanumGothic.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception e) {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        }
    }
}