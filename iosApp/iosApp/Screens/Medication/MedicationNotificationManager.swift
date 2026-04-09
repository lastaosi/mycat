import Foundation
import UserNotifications

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - MedicationNotificationManager
// Android AlarmManager(MedicationAlarmScheduler) 의 iOS 대응 클래스.
//
// UNUserNotificationCenter 를 사용해 매일 반복 알람을 등록·취소한다.
//
// 알람 identifier 규칙:
//   "medication_\(medicationId)_\(alarmTime)"
//   예: "medication_3_08:00"
//
// 이 규칙 덕분에 medicationId + alarmTime 조합만 알면 언제든 취소 가능.
// ─────────────────────────────────────────────────────────────────────────────

final class MedicationNotificationManager {

    static let shared = MedicationNotificationManager()
    private init() {}

    // ── 알람 identifier prefix ──────────────────────────────────────────────
    private let identifierPrefix = "medication_"

    // MARK: - 권한 요청
    // iOSApp.swift 또는 AppRoot.onAppear 에서 한 번만 호출한다.

    func requestAuthorization(completion: ((Bool) -> Void)? = nil) {
        UNUserNotificationCenter.current().requestAuthorization(
            options: [.alert, .sound, .badge]
        ) { granted, error in
            if let error = error {
                print("MedicationNotificationManager: 권한 요청 오류 - \(error.localizedDescription)")
            }
            print("MedicationNotificationManager: 알림 권한 \(granted ? "허용" : "거부")")
            DispatchQueue.main.async {
                completion?(granted)
            }
        }
    }

    // MARK: - 알람 등록
    // Android MedicationAlarmScheduler.scheduleAlarm 대응.
    // DAILY 반복: UNCalendarNotificationTrigger (매일 같은 시/분)
    //
    // @param medicationId  약 ID (DB 기본키)
    // @param catName       고양이 이름 (알림 본문에 표시)
    // @param medName       약 이름 (알림 제목에 표시)
    // @param alarmTimes    ["08:00", "21:00"] 형식의 알람 시간 배열

    func scheduleAlarms(
        medicationId: Int64,
        catName: String,
        medName: String,
        alarmTimes: [String]
    ) {
        UNUserNotificationCenter.current().getNotificationSettings { [weak self] settings in
            guard let self = self else { return }
            guard settings.authorizationStatus == .authorized ||
                  settings.authorizationStatus == .provisional else {
                print("MedicationNotificationManager: 알림 권한 없음 - 알람 등록 건너뜀")
                return
            }

            for timeStr in alarmTimes {
                self.scheduleSingleAlarm(
                    medicationId: medicationId,
                    catName: catName,
                    medName: medName,
                    alarmTime: timeStr
                )
            }
        }
    }

    private func scheduleSingleAlarm(
        medicationId: Int64,
        catName: String,
        medName: String,
        alarmTime: String  // "HH:mm"
    ) {
        let parts = alarmTime.split(separator: ":").map { Int($0) ?? 0 }
        guard parts.count == 2 else {
            print("MedicationNotificationManager: 잘못된 시간 형식 - \(alarmTime)")
            return
        }
        let hour = parts[0]
        let minute = parts[1]

        // ─ 알림 콘텐츠 ─────────────────────────────────────────────────────
        let content = UNMutableNotificationContent()
        content.title = "💊 \(medName) 복용 시간이에요"
        content.body = catName.isEmpty
            ? "\(alarmTime) 약 복용을 잊지 마세요!"
            : "\(catName)의 \(medName) (\(alarmTime)) 복용 시간이에요 🐱"
        content.sound = .default

        // ─ 매일 반복 트리거 (UNCalendarNotificationTrigger) ────────────────
        var dateComponents = DateComponents()
        dateComponents.hour = hour
        dateComponents.minute = minute

        let trigger = UNCalendarNotificationTrigger(
            dateMatching: dateComponents,
            repeats: true  // 매일 반복
        )

        // ─ 요청 등록 ────────────────────────────────────────────────────────
        let identifier = makeIdentifier(medicationId: medicationId, alarmTime: alarmTime)
        let request = UNNotificationRequest(
            identifier: identifier,
            content: content,
            trigger: trigger
        )

        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("MedicationNotificationManager: 알람 등록 실패 [\(identifier)] - \(error.localizedDescription)")
            } else {
                print("MedicationNotificationManager: 알람 등록 성공 [\(identifier)] \(hour):\(String(format: "%02d", minute))")
            }
        }
    }

    // MARK: - 알람 취소 (alarmIds 기반)
    // DB 의 MedicationAlarm.id 목록을 받아 해당 알람을 취소한다.
    // identifier 는 medicationId + alarmTime 조합이므로,
    // 실제로는 medicationId 기반 prefix 로 일괄 취소하는 방식을 사용.

    func cancelAlarms(medicationId: Int64) {
        UNUserNotificationCenter.current().getPendingNotificationRequests { [weak self] requests in
            guard let self = self else { return }
            let prefix = self.makePrefix(medicationId: medicationId)
            let toRemove = requests
                .filter { $0.identifier.hasPrefix(prefix) }
                .map { $0.identifier }

            if !toRemove.isEmpty {
                UNUserNotificationCenter.current()
                    .removePendingNotificationRequests(withIdentifiers: toRemove)
                print("MedicationNotificationManager: 알람 취소 [\(toRemove.joined(separator: ", "))]")
            }
        }
    }

    // MARK: - 특정 시간 알람만 취소

    func cancelAlarm(medicationId: Int64, alarmTime: String) {
        let identifier = makeIdentifier(medicationId: medicationId, alarmTime: alarmTime)
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: [identifier])
        print("MedicationNotificationManager: 개별 알람 취소 [\(identifier)]")
    }

    // MARK: - 전체 약 알람 취소 (앱 초기화 등 특수 상황)

    func cancelAllMedicationAlarms() {
        UNUserNotificationCenter.current().getPendingNotificationRequests { [weak self] requests in
            guard let self = self else { return }
            let toRemove = requests
                .filter { $0.identifier.hasPrefix(self.identifierPrefix) }
                .map { $0.identifier }
            UNUserNotificationCenter.current()
                .removePendingNotificationRequests(withIdentifiers: toRemove)
            print("MedicationNotificationManager: 전체 약 알람 취소 (\(toRemove.count)개)")
        }
    }

    // MARK: - Helpers

    private func makeIdentifier(medicationId: Int64, alarmTime: String) -> String {
        "\(identifierPrefix)\(medicationId)_\(alarmTime)"
    }

    private func makePrefix(medicationId: Int64) -> String {
        "\(identifierPrefix)\(medicationId)_"
    }
}
