package com.kids.services;

import com.kids.entities.Employee;
import com.kids.entities.EmployeePayment;
import com.kids.entities.PaymentMethod;
import com.kids.entities.PaymentStatus;
import com.kids.repositories.EmployeeAttendanceRepository;
import com.kids.repositories.EmployeePaymentRepository;
import com.kids.repositories.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static com.kids.entities.SalaryType.FIXED_MONTHLY;
import static com.kids.entities.SalaryType.PER_SESSION;

@Service
public class EmployeeService {
    @Autowired
    EmployeeRepository employeeRepository;
    @Autowired
    EmployeePaymentRepository employeePaymentRepository;

    @Autowired
    EmployeeAttendanceRepository attendanceRepo;

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public void save(Employee employee) {
        employeeRepository.save(employee);
    }

    public void deleteById(Long id) {
        employeeRepository.deleteById(id);
    }

    public BigDecimal calculatePendingSalary(Long employeeId, YearMonth yearMonth) {
        BigDecimal gross = calculateGrossSalary(employeeId, yearMonth);
        BigDecimal alreadyPaid = BigDecimal.ZERO;
        BigDecimal pending = gross.subtract(alreadyPaid);
        return pending.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : pending;
    }


    public BigDecimal calculateGrossSalary(Long employeeId, YearMonth yearMonth) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        LocalDate from = yearMonth.atDay(1);
        LocalDate to = yearMonth.atEndOfMonth();

        return switch (employee.getSalaryType()) {

            case FIXED_MONTHLY -> {
                long absences = attendanceRepo.countAbsencesByMonth(employeeId, from, to);
                BigDecimal gross = employee.getBaseSalary();
                // Floor at zero — never a negative salary
                yield gross.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : gross;
            }

            case PER_SESSION -> {
                long sessions = attendanceRepo.sumSessionsByMonth(employeeId, from, to);
                yield employee.getBaseSalary().multiply(BigDecimal.valueOf(sessions));
            }
        };
    }

    @Transactional
    public EmployeePayment recordPayment(Long employeeId,
                                         YearMonth yearMonth,
                                         BigDecimal amountToPay,
                                         PaymentMethod method,
                                         String reference) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        BigDecimal gross   = calculateGrossSalary(employeeId, yearMonth);
        BigDecimal pending = calculatePendingSalary(employeeId, yearMonth);

        if (amountToPay.compareTo(pending) > 0) {
            throw new IllegalStateException(
                    "Payment amount %s exceeds pending salary %s for month %s"
                            .formatted(amountToPay, pending, yearMonth)
            );
        }

        // Determine if this is a full settlement or an advance
        BigDecimal afterThisPayment = pending.subtract(amountToPay);
        PaymentStatus status =
                afterThisPayment.compareTo(BigDecimal.ZERO) == 0
                        ? PaymentStatus.PAID
                        : PaymentStatus.ADVANCE;

        EmployeePayment payment = EmployeePayment.builder()
                .employee(employee)
                .coveredMonth(yearMonth.toString())
                .grossAmount(gross)
                .deductions(gross.subtract(pending))   // total deductions so far
                .netAmount(amountToPay)
                .paymentDate(LocalDate.now())
                .paymentMethod(method)
                .paymentStatus(status)
                .reference(reference)
                .build();

        EmployeePayment saved = employeePaymentRepository.save(payment);

        return saved;
    }

}
