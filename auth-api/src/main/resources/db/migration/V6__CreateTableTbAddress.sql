create table tb_address (
    id serial not null,
    user_id bigint not null references tb_user (id),
    label varchar(50),
    zip_code varchar(8) not null,
    street varchar(255) not null,
    number varchar(20) not null,
    complement varchar(255),
    district varchar(255) not null,
    city varchar(255) not null,
    state char(2) not null,
    primary key (id)
)
