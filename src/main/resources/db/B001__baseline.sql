create table bank_statement_setting
(
    id                    bigint auto_increment
        primary key,
    amount_col_index      int          not null,
    bank_name             varchar(255) not null,
    client_name_col_index int          not null,
    date_col_index        int          not null,
    date_format           varchar(255) not null,
    start_row             int          not null,
    subject_col_index     int          not null,
    constraint UKsfcyb8ekhqji6vs0jwm8txvmd
        unique (bank_name)
)
    charset = utf8mb4;

create table client
(
    id   int          not null
        primary key,
    name varchar(255) null,
    constraint UK_dn5jasds5r1j3ewo5k3nhwkkq
        unique (name)
)
    charset = utf8mb4;

create table client_seq
(
    next_val bigint null
)
    charset = utf8mb4;

create table color_mapping
(
    id              bigint auto_increment
        primary key,
    description     varchar(255) null,
    excel_argb_hex  varchar(255) not null,
    html_color_code varchar(255) not null,
    constraint UKbu56l7suv27qkn4g8c1j166qn
        unique (excel_argb_hex)
)
    charset = utf8mb4;

create table global_setting
(
    setting_key   varchar(255) not null
        primary key,
    setting_value varchar(255) null
)
    charset = utf8mb4;

create table invoice_parse_setting
(
    id                   bigint auto_increment
        primary key,
    start_row            int not null comment 'Номер строки начала данных (0-based)',
    article_col          int not null comment 'Номер колонки с артикулом (0-based)',
    quantity_col         int not null comment 'Номер колонки с количеством (0-based)',
    item_number_col      int not null,
    supplier_article_col int not null
)
    charset = utf8mb4;

create table invoice_seq
(
    next_val bigint null
)
    charset = utf8mb4;

create table role_mapping
(
    id            bigint auto_increment
        primary key,
    app_role      varchar(255) not null,
    ad_group_name varchar(255) not null,
    constraint uk_app_role
        unique (app_role)
)
    charset = utf8mb4;

create table salary_column_mapping
(
    id              bigint auto_increment
        primary key,
    column_name     varchar(255)                     not null,
    excel_col_index int                              not null,
    is_salary       bit                              not null,
    use_excel_color bit                              not null,
    is_visible      bit                              not null,
    is_currency     bit                              not null,
    alignment       enum ('CENTER', 'LEFT', 'RIGHT') null
)
    charset = utf8mb4;

create table salary_parse_setting
(
    id             bigint auto_increment
        primary key,
    date_col_index int not null,
    start_row      int not null
)
    charset = utf8mb4;

create table supplier_setting
(
    id         bigint auto_increment
        primary key,
    title      varchar(255) not null comment 'Наименование поставщика',
    color_name varchar(255) null comment 'Текстовое название цвета (для отображения)',
    color_hex  varchar(255) null comment 'ARGB HEX код цвета из Excel (ключ для поиска)',
    file_name  varchar(255) not null comment 'Имя выходного файла для заказа',
    is_default bit          null,
    constraint uk_color_hex
        unique (color_hex)
)
    charset = utf8mb4;

create table user_group
(
    id    int         not null
        primary key,
    alias varchar(20) null,
    constraint UK_m3ao9xvq6psewnimq59l5avrh
        unique (alias)
)
    charset = utf8mb4;

create table user
(
    id            int           not null
        primary key,
    user_group_id int           null,
    login         varchar(50)   null,
    percent       decimal(5, 2) null,
    constraint UK_ew1hvam8uwaknuaellwhqchhb
        unique (login),
    constraint FKd5uhmsqhax1l70pck9lmgphjr
        foreign key (user_group_id) references user_group (id)
)
    charset = utf8mb4;

create table invoice
(
    arrival_date     date           null,
    calculated_sum   decimal(15, 2) null,
    client_id        int            null,
    create_date      date           null,
    discount         smallint       null,
    given_date       date           null,
    given_sum        decimal(15, 2) null,
    purchase_sum     decimal(15, 2) null,
    shipment_date    date           null,
    sum              decimal(15, 2) null,
    user_id          int            null,
    id               bigint         not null
        primary key,
    documents_status varchar(50)    null,
    notes            varchar(100)   null,
    number           varchar(150)   null,
    constraint FK6y01j0975eqwmnb0gckttrbj2
        foreign key (client_id) references client (id),
    constraint FKjunvl5maki3unqdvljk31kns3
        foreign key (user_id) references user (id)
)
    charset = utf8mb4;

create table user_group_seq
(
    next_val bigint null
)
    charset = utf8mb4;

create table user_seq
(
    next_val bigint null
)
    charset = utf8mb4;

