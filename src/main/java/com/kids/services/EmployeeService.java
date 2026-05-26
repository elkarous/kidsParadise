package com.kids.services;

import com.kids.entities.*;
import com.kids.repositories.EmployeeAttendanceRepository;
import com.kids.repositories.EmployeePaymentRepository;
import com.kids.repositories.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeePaymentRepository employeePaymentRepository;

    @Autowired
    private EmployeeAttendanceRepository attendanceRepo;

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public void save(Employee employee) {
        employeeRepository.save(employee);
    }

    public void deleteById(Long id) {
        employeeRepository.deleteById(id);
    }

    /**
     * 📊 حساب الراتب الإجمالي بناءً على كائن WorkingMonth الدراسي
     */
    public BigDecimal calculateGrossSalary(Long employeeId, WorkingMonth workingMonth) {
        if (workingMonth == null) {
            throw new IllegalArgumentException("Working month cannot be null");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        // تحويل كائن التكوين الدراسي إلى فترات زمنية محلية لحساب الحضور والغياب
        YearMonth yearMonth = YearMonth.of(workingMonth.getYear(), workingMonth.getMonthNumber());
        LocalDate from = yearMonth.atDay(1);
        LocalDate to = yearMonth.atEndOfMonth();

        return switch (employee.getSalaryType()) {
            case FIXED_MONTHLY -> {
                long absences = attendanceRepo.countAbsencesByMonth(employeeId, from, to);
                BigDecimal gross = employee.getBaseSalary();
                // يمكن هنا مستقبلاً خصم الغيابات (مثال: gross.subtract(خصم))
                yield gross.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : gross;
            }
            case PER_SESSION -> {
                long sessions = attendanceRepo.sumSessionsByMonth(employeeId, from, to);
                yield employee.getBaseSalary().multiply(BigDecimal.valueOf(sessions));
            }
        };
    }

    /**
     * 💰 حساب المبلغ المتبقي المستحق للصرف بناءً على المعرف الفعلي للشهر الدراسي
     */
    public BigDecimal calculatePendingSalary(Long employeeId, WorkingMonth workingMonth) {
        if (workingMonth == null) return BigDecimal.ZERO;

        BigDecimal gross = calculateGrossSalary(employeeId, workingMonth);

        // جلب مجموع المدفوعات المسجلة لهذا الشهر الدراسي عبر المعرف الخاص به (workingMonth.getId())
        BigDecimal alreadyPaid = employeePaymentRepository.sumPaidAmountByMonth(employeeId, workingMonth.getId());
        if (alreadyPaid == null) {
            alreadyPaid = BigDecimal.ZERO;
        }

        BigDecimal pending = gross.subtract(alreadyPaid);
        return pending.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : pending;
    }

    /**
     * 💳 تسجيل حركة صرف مالي وربطها بالشهر الدراسي الفعلي
     */
    @Transactional
    public EmployeePayment recordPayment(Long employeeId,
                                         WorkingMonth workingMonth,
                                         BigDecimal amountToPay,
                                         PaymentMethod method,
                                         String reference) {

        if (workingMonth == null) {
            throw new IllegalArgumentException("Working month cannot be null");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        BigDecimal gross   = calculateGrossSalary(employeeId, workingMonth);
        BigDecimal pending = calculatePendingSalary(employeeId, workingMonth);

        if (amountToPay.compareTo(pending) > 0) {
            throw new IllegalStateException(
                    "Payment amount %s exceeds pending salary %s for month %s"
                            .formatted(amountToPay, pending, workingMonth.getMonthName())
            );
        }

        BigDecimal afterThisPayment = pending.subtract(amountToPay);
        PaymentStatus status = afterThisPayment.compareTo(BigDecimal.ZERO) == 0
                ? PaymentStatus.PAID
                : PaymentStatus.ADVANCE;

        // بناء حركة الصرف وربطها بالـ Entity بشكل صحيح لمنع الـ NullPointerException
        EmployeePayment payment = EmployeePayment.builder()
                .employee(employee)
                .workingMonth(workingMonth) // 🟢 تم إصلاح الحقن هنا
                .grossAmount(gross)
                .deductions(gross.subtract(pending)) // مجموع الدفوعات السابقة أو الخصومات المتراكمة
                .netAmount(amountToPay)
                .paymentDate(LocalDate.now())
                .paymentMethod(method)
                .paymentStatus(status)
                .reference(reference)
                .build();

        return employeePaymentRepository.save(payment);
    }

    public Optional<EmployeeAttendance> findAttendanceByDateAndId(Long id, LocalDate selectedDate) {
        return attendanceRepo.findByEmployeeIdAndAndAttendanceDate(id, selectedDate);
    }

    @Transactional
    public void saveAttendance(Employee employee, LocalDate date, AttendanceStatus status, LocalTime checkIn, LocalTime checkOut, String notes) {
        EmployeeAttendance attendance = attendanceRepo
                .findByEmployeeIdAndAndAttendanceDate(employee.getId(), date)
                .orElseGet(() -> EmployeeAttendance.builder()
                        .employee(employee)
                        .attendanceDate(date)
                        .build());

        attendance.setStatus(status);
        attendance.setCheckInTime(checkIn);
        attendance.setCheckOutTime(checkOut);
        attendance.setNotes(notes);

        attendanceRepo.save(attendance);
    }
}