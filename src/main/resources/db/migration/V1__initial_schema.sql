-- KinderERP initial schema (SQLite).
-- Conventions: dates are ISO text (yyyy-MM-dd), times are "HH:mm" text,
-- money is an INTEGER number of thousandths (12.500 -> 12500), never a floating-point value.

create table school_settings (
    id                           bigint       not null primary key check (id = 1),
    name_fr                      varchar(200) not null,
    name_ar                      varchar(200) not null,
    address                      varchar(300),
    phone                        varchar(50),
    email                        varchar(150),
    logo                         blob,
    region                       varchar(2)   not null default 'TN',
    currency_symbol              varchar(10)  not null,
    currency_decimals            integer      not null default 3,
    monthly_fee                  bigint       not null default 0,
    discount_two_children        bigint       not null default 0,
    discount_three_plus_children bigint       not null default 0,
    year_start_month             integer      not null default 9 check (year_start_month between 1 and 12),
    year_end_month               integer      not null default 6 check (year_end_month between 1 and 12),
    setup_completed              boolean      not null default 0,
    trial_start_date             varchar(10),
    license_key                  varchar(200)
);

create table users (
    id            integer primary key,
    username      varchar(50)  not null unique collate nocase,
    full_name     varchar(150),
    password_hash varchar(100) not null,
    active        boolean      not null default 1
);

create table school_years (
    id         integer primary key,
    name       varchar(20) not null unique,
    start_date varchar(10) not null,
    end_date   varchar(10) not null
);

create table working_months (
    id             integer primary key,
    school_year_id integer  not null references school_years (id) on delete cascade,
    calendar_year  integer not null,
    calendar_month integer not null check (calendar_month between 1 and 12),
    closed         boolean not null default 0,
    unique (school_year_id, calendar_year, calendar_month)
);

create table levels (
    id   integer primary key,
    name varchar(100) not null
);

create table teachers (
    id              integer primary key,
    name            varchar(150) not null,
    specialty       varchar(100),
    phone           varchar(30),
    email           varchar(150),
    hiring_date     varchar(10)  not null,
    salary_type     varchar(20)  not null check (salary_type in ('FIXED_MONTHLY', 'PER_SESSION')),
    base_salary     bigint       not null default 0,
    absence_penalty bigint       not null default 0,
    status          varchar(20)  not null check (status in ('ACTIVE', 'INACTIVE', 'ON_LEAVE'))
);

create table employees (
    id              integer primary key,
    name            varchar(150) not null,
    job_title       varchar(100),
    phone           varchar(30),
    email           varchar(150),
    hiring_date     varchar(10)  not null,
    salary_type     varchar(20)  not null check (salary_type in ('FIXED_MONTHLY', 'PER_SESSION')),
    base_salary     bigint       not null default 0,
    absence_penalty bigint       not null default 0,
    status          varchar(20)  not null check (status in ('ACTIVE', 'INACTIVE', 'ON_LEAVE'))
);

create table school_classes (
    id         integer primary key,
    name       varchar(100) not null,
    level_id   integer       not null references levels (id) on delete restrict,
    teacher_id integer references teachers (id) on delete set null
);
create index idx_school_classes_level on school_classes (level_id);

create table parents (
    id          integer primary key,
    father_name varchar(150) not null,
    mother_name varchar(150),
    phone       varchar(30)  not null,
    email       varchar(150)
);

create table students (
    id              integer primary key,
    first_name      varchar(100) not null,
    last_name       varchar(100) not null,
    birth_date      varchar(10)  not null,
    enrollment_date varchar(10)  not null,
    parent_id       integer references parents (id) on delete restrict,
    class_id        integer references school_classes (id) on delete restrict
);
create index idx_students_parent on students (parent_id);
create index idx_students_class on students (class_id);

create table student_attendance (
    id              integer primary key,
    student_id      integer      not null references students (id) on delete cascade,
    class_id        integer      not null references school_classes (id) on delete restrict,
    attendance_date varchar(10) not null,
    status          varchar(20) not null check (status in ('PRESENT', 'LATE', 'ABSENT', 'EXCUSED')),
    notes           varchar(255),
    unique (student_id, attendance_date)
);
create index idx_student_attendance_class_date on student_attendance (class_id, attendance_date);

create table tuition_payments (
    id               integer primary key,
    parent_id        integer      not null references parents (id) on delete restrict,
    working_month_id integer      not null references working_months (id) on delete restrict,
    children_count   integer     not null,
    amount           bigint      not null,
    discount         bigint      not null default 0,
    payment_date     varchar(10) not null,
    method           varchar(20) not null check (method in ('CASH', 'CHEQUE', 'BANK_TRANSFER')),
    notes            varchar(255),
    unique (parent_id, working_month_id)
);
create index idx_tuition_payments_month on tuition_payments (working_month_id);

create table teacher_attendance (
    id              integer primary key,
    teacher_id      integer      not null references teachers (id) on delete cascade,
    attendance_date varchar(10) not null,
    status          varchar(20) not null check (status in ('PRESENT', 'LATE', 'ABSENT', 'EXCUSED')),
    sessions_count  integer     not null default 1,
    check_in        varchar(5),
    check_out       varchar(5),
    notes           varchar(255),
    unique (teacher_id, attendance_date)
);
create index idx_teacher_attendance_date on teacher_attendance (attendance_date);

create table employee_attendance (
    id              integer primary key,
    employee_id     integer      not null references employees (id) on delete cascade,
    attendance_date varchar(10) not null,
    status          varchar(20) not null check (status in ('PRESENT', 'LATE', 'ABSENT', 'EXCUSED')),
    sessions_count  integer     not null default 1,
    check_in        varchar(5),
    check_out       varchar(5),
    notes           varchar(255),
    unique (employee_id, attendance_date)
);
create index idx_employee_attendance_date on employee_attendance (attendance_date);

create table teacher_payments (
    id               integer primary key,
    teacher_id       integer      not null references teachers (id) on delete restrict,
    working_month_id integer      not null references working_months (id) on delete restrict,
    gross_amount     bigint      not null,
    amount           bigint      not null,
    payment_date     varchar(10) not null,
    method           varchar(20) not null check (method in ('CASH', 'CHEQUE', 'BANK_TRANSFER')),
    status           varchar(20) not null check (status in ('PAID', 'ADVANCE')),
    reference        varchar(100),
    notes            varchar(255)
);
create index idx_teacher_payments_teacher_month on teacher_payments (teacher_id, working_month_id);

create table employee_payments (
    id               integer primary key,
    employee_id      integer      not null references employees (id) on delete restrict,
    working_month_id integer      not null references working_months (id) on delete restrict,
    gross_amount     bigint      not null,
    amount           bigint      not null,
    payment_date     varchar(10) not null,
    method           varchar(20) not null check (method in ('CASH', 'CHEQUE', 'BANK_TRANSFER')),
    status           varchar(20) not null check (status in ('PAID', 'ADVANCE')),
    reference        varchar(100),
    notes            varchar(255)
);
create index idx_employee_payments_employee_month on employee_payments (employee_id, working_month_id);
