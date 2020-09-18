SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;


CREATE EXTENSION IF NOT EXISTS file_fdw WITH SCHEMA public;
COMMENT ON EXTENSION file_fdw IS 'foreign-data wrapper for flat file access';
CREATE EXTENSION IF NOT EXISTS hstore WITH SCHEMA public;
COMMENT ON EXTENSION hstore IS 'data type for storing sets of (key, value) pairs';
CREATE EXTENSION IF NOT EXISTS pg_stat_statements WITH SCHEMA public;
COMMENT ON EXTENSION pg_stat_statements IS 'track execution statistics of all SQL statements executed';
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';

CREATE FUNCTION public.is_date(s character varying) RETURNS boolean
    LANGUAGE plpgsql
    AS $$
begin
  perform coalesce(s, '')::date;
  return true;
exception when others then
  return false;
end;
$$;

CREATE SERVER fileserver FOREIGN DATA WRAPPER file_fdw;


SET default_tablespace = '';
SET default_with_oids = false;

--
-- Name: customer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer (
    id integer NOT NULL,
    first_name varchar(50) NOT NULL,
    last_name varchar(50) NOT NULL,
    email varchar(200) NOT NULL,
    phone_number char(10) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE SEQUENCE public.customer_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.customer_id_seq OWNED BY public.customer.id;

--
-- Name: address; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.address (
    id integer NOT NULL,
    customer_id integer not null,
    address varchar(100) not null,
    city varchar(100) not null,
    state char(2) not null,
    zip_code char(5) not null,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE SEQUENCE public.address_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.address_id_seq OWNED BY public.address.id;

ALTER TABLE ONLY public.customer ADD CONSTRAINT customer_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.address ADD CONSTRAINT address_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.customer ALTER COLUMN id SET DEFAULT nextval('public.customer_id_seq'::regclass);
ALTER TABLE ONLY public.address ALTER COLUMN id SET DEFAULT nextval('public.address_id_seq'::regclass);

ALTER TABLE ONLY public.address ADD CONSTRAINT address_customer_fk FOREIGN KEY (customer_id) REFERENCES public.customer(id);
CREATE INDEX address_customer_id_idx ON public.address USING btree (customer_id);
