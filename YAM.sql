--
-- PostgreSQL database dump
--

-- Dumped from database version 17.5
-- Dumped by pg_dump version 17.5

-- Started on 2025-09-08 10:49:25

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 2 (class 3079 OID 25011)
-- Name: pg_trgm; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;


--
-- TOC entry 5082 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION pg_trgm; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pg_trgm IS 'text similarity measurement and index searching based on trigrams';


--
-- TOC entry 932 (class 1247 OID 24983)
-- Name: chat_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.chat_type AS ENUM (
    'private',
    'group',
    'channel'
);


ALTER TYPE public.chat_type OWNER TO postgres;

--
-- TOC entry 920 (class 1247 OID 16731)
-- Name: message_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.message_status AS ENUM (
    'sent',
    'delivered',
    'read'
);


ALTER TYPE public.message_status OWNER TO postgres;

--
-- TOC entry 908 (class 1247 OID 16664)
-- Name: role_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.role_type AS ENUM (
    'member',
    'admin',
    'owner'
);


ALTER TYPE public.role_type OWNER TO postgres;

--
-- TOC entry 235 (class 1255 OID 24996)
-- Name: chat_search_update(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.chat_search_update() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    title TEXT;
BEGIN
    IF NEW.chat_type = 'channel' THEN
        SELECT channel_name INTO title
        FROM channel
        WHERE chat_id = NEW.chat_id;

    ELSIF NEW.chat_type = 'group' THEN
        SELECT group_name INTO title
        FROM group_chat
        WHERE chat_id = NEW.chat_id;
    END IF;

    NEW.search_vector := to_tsvector('simple', COALESCE(title, ''));
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.chat_search_update() OWNER TO postgres;

--
-- TOC entry 234 (class 1255 OID 25005)
-- Name: log_channel_insert(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.log_channel_insert() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    INSERT INTO audit_log (chat_id, action)
    VALUES (NEW.chat_id, 'insert'); -- حذف description
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.log_channel_insert() OWNER TO postgres;

--
-- TOC entry 233 (class 1255 OID 16780)
-- Name: messages_tsvector_trigger(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.messages_tsvector_trigger() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
  new.search_vector := to_tsvector('simple', coalesce(new.message_text,''));
  return new;
end
$$;


ALTER FUNCTION public.messages_tsvector_trigger() OWNER TO postgres;

--
-- TOC entry 236 (class 1255 OID 25008)
-- Name: set_chat_name(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.set_chat_name() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    target_name TEXT;
BEGIN
    IF NEW.chat_type = 'private' THEN
        SELECT p.profile_name INTO target_name
        FROM private_chat pc
        JOIN users u ON pc.user2_id = u.user_id
        JOIN user_profiles p ON u.user_id = p.user_id
        WHERE pc.chat_id = NEW.chat_id;

        NEW.name := target_name;

    ELSIF NEW.chat_type = 'group' THEN
        SELECT gc.group_name INTO NEW.name
        FROM group_chat gc
        WHERE gc.chat_id = NEW.chat_id;

    ELSIF NEW.chat_type = 'channel' THEN
        SELECT ch.channel_name INTO NEW.name
        FROM channel ch
        WHERE ch.chat_id = NEW.chat_id;
    END IF;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.set_chat_name() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 226 (class 1259 OID 16693)
-- Name: channel; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.channel (
    chat_id bigint NOT NULL,
    channel_name character varying(100) NOT NULL,
    description text,
    owner_id bigint NOT NULL,
    is_private boolean DEFAULT false,
    avatar_url text,
    search_vector tsvector
);


ALTER TABLE public.channel OWNER TO postgres;

--
-- TOC entry 227 (class 1259 OID 16712)
-- Name: channel_subscribers; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.channel_subscribers (
    chat_id bigint NOT NULL,
    user_id bigint NOT NULL,
    role public.role_type DEFAULT 'member'::public.role_type,
    joined_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    approved boolean DEFAULT true
);


ALTER TABLE public.channel_subscribers OWNER TO postgres;

--
-- TOC entry 223 (class 1259 OID 16613)
-- Name: chat; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.chat (
    chat_id bigint NOT NULL,
    chat_type character varying(10) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    search_vector tsvector,
    name text,
    CONSTRAINT chat_chat_type_check CHECK (((chat_type)::text = ANY (ARRAY[('private'::character varying)::text, ('group'::character varying)::text, ('channel'::character varying)::text])))
);


ALTER TABLE public.chat OWNER TO postgres;

--
-- TOC entry 222 (class 1259 OID 16612)
-- Name: chat_chat_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.chat_chat_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.chat_chat_id_seq OWNER TO postgres;

--
-- TOC entry 5083 (class 0 OID 0)
-- Dependencies: 222
-- Name: chat_chat_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.chat_chat_id_seq OWNED BY public.chat.chat_id;


--
-- TOC entry 230 (class 1259 OID 16788)
-- Name: contacts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.contacts (
    owner_id bigint NOT NULL,
    contact_id bigint NOT NULL,
    added_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.contacts OWNER TO postgres;

--
-- TOC entry 224 (class 1259 OID 16644)
-- Name: group_chat; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.group_chat (
    chat_id bigint NOT NULL,
    group_name character varying(100) NOT NULL,
    description text,
    creator_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    is_private boolean DEFAULT false,
    group_avatar_url text,
    search_vector tsvector
);


ALTER TABLE public.group_chat OWNER TO postgres;

--
-- TOC entry 225 (class 1259 OID 16671)
-- Name: group_members; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.group_members (
    chat_id bigint NOT NULL,
    user_id bigint NOT NULL,
    role public.role_type DEFAULT 'member'::public.role_type,
    joined_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    invited_by bigint
);


ALTER TABLE public.group_members OWNER TO postgres;

--
-- TOC entry 229 (class 1259 OID 16738)
-- Name: messages; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.messages (
    message_id bigint NOT NULL,
    chat_id bigint,
    sender_id bigint,
    message_text text NOT NULL,
    message_type character varying(20) DEFAULT 'text'::character varying,
    reply_to_message_id bigint,
    forwarded_from_message_id bigint,
    is_edited boolean DEFAULT false,
    is_deleted boolean DEFAULT false,
    status public.message_status DEFAULT 'sent'::public.message_status,
    sent_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    search_vector tsvector,
    CONSTRAINT messages_message_type_check CHECK (((message_type)::text = ANY ((ARRAY['text'::character varying, 'photo'::character varying, 'video'::character varying, 'audio'::character varying, 'document'::character varying, 'location'::character varying, 'contact'::character varying, 'voice'::character varying])::text[])))
);


ALTER TABLE public.messages OWNER TO postgres;

--
-- TOC entry 228 (class 1259 OID 16737)
-- Name: messages_message_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.messages_message_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.messages_message_id_seq OWNER TO postgres;

--
-- TOC entry 5084 (class 0 OID 0)
-- Dependencies: 228
-- Name: messages_message_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.messages_message_id_seq OWNED BY public.messages.message_id;


--
-- TOC entry 232 (class 1259 OID 24976)
-- Name: private_chat; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.private_chat (
    chat_id bigint NOT NULL,
    user1_id bigint NOT NULL,
    user2_id bigint NOT NULL
);


ALTER TABLE public.private_chat OWNER TO postgres;

--
-- TOC entry 231 (class 1259 OID 24975)
-- Name: private_chat_chat_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.private_chat_chat_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.private_chat_chat_id_seq OWNER TO postgres;

--
-- TOC entry 5085 (class 0 OID 0)
-- Dependencies: 231
-- Name: private_chat_chat_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.private_chat_chat_id_seq OWNED BY public.private_chat.chat_id;


--
-- TOC entry 221 (class 1259 OID 16596)
-- Name: user_profiles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_profiles (
    profile_id bigint NOT NULL,
    user_id bigint,
    profile_image_url text,
    bio character varying(150),
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    username character varying(25),
    password text,
    profile_name character varying(25),
    search_vector tsvector
);


ALTER TABLE public.user_profiles OWNER TO postgres;

--
-- TOC entry 220 (class 1259 OID 16595)
-- Name: user_profiles_profile_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_profiles_profile_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.user_profiles_profile_id_seq OWNER TO postgres;

--
-- TOC entry 5086 (class 0 OID 0)
-- Dependencies: 220
-- Name: user_profiles_profile_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_profiles_profile_id_seq OWNED BY public.user_profiles.profile_id;


--
-- TOC entry 219 (class 1259 OID 16560)
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    user_id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    last_seen timestamp with time zone,
    is_verified boolean DEFAULT false,
    is_online boolean DEFAULT false,
    is_deleted boolean DEFAULT false,
    email character varying(255) NOT NULL
);


ALTER TABLE public.users OWNER TO postgres;

--
-- TOC entry 218 (class 1259 OID 16559)
-- Name: users_user_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.users_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.users_user_id_seq OWNER TO postgres;

--
-- TOC entry 5087 (class 0 OID 0)
-- Dependencies: 218
-- Name: users_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.users_user_id_seq OWNED BY public.users.user_id;


--
-- TOC entry 4850 (class 2604 OID 16616)
-- Name: chat chat_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat ALTER COLUMN chat_id SET DEFAULT nextval('public.chat_chat_id_seq'::regclass);


--
-- TOC entry 4860 (class 2604 OID 16741)
-- Name: messages message_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages ALTER COLUMN message_id SET DEFAULT nextval('public.messages_message_id_seq'::regclass);


--
-- TOC entry 4867 (class 2604 OID 24979)
-- Name: private_chat chat_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.private_chat ALTER COLUMN chat_id SET DEFAULT nextval('public.private_chat_chat_id_seq'::regclass);


--
-- TOC entry 4848 (class 2604 OID 16599)
-- Name: user_profiles profile_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles ALTER COLUMN profile_id SET DEFAULT nextval('public.user_profiles_profile_id_seq'::regclass);


--
-- TOC entry 4843 (class 2604 OID 16563)
-- Name: users user_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users ALTER COLUMN user_id SET DEFAULT nextval('public.users_user_id_seq'::regclass);


--
-- TOC entry 5070 (class 0 OID 16693)
-- Dependencies: 226
-- Data for Name: channel; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.channel (chat_id, channel_name, description, owner_id, is_private, avatar_url, search_vector) FROM stdin;
83	mimimio	hello wordl	12	f	\N	\N
\.


--
-- TOC entry 5071 (class 0 OID 16712)
-- Dependencies: 227
-- Data for Name: channel_subscribers; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.channel_subscribers (chat_id, user_id, role, joined_at, approved) FROM stdin;
83	12	member	2025-09-07 18:12:05.416749	t
\.


--
-- TOC entry 5067 (class 0 OID 16613)
-- Dependencies: 223
-- Data for Name: chat; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.chat (chat_id, chat_type, created_at, search_vector, name) FROM stdin;
68	group	2025-09-07 17:43:00.30804		\N
69	group	2025-09-07 17:43:09.474648		\N
70	group	2025-09-07 17:45:54.198791		\N
71	group	2025-09-07 17:48:52.333049		\N
72	group	2025-09-07 17:49:51.746274		\N
73	channel	2025-09-07 17:55:57.319801	'mimimio':1	mimimio
74	channel	2025-09-07 17:56:10.891885	'mimimio':1	mimimio
75	channel	2025-09-07 17:57:33.506351	'mimimio':1	mimimio
76	channel	2025-09-07 17:59:19.402232	'mimimio':1	mimimio
77	channel	2025-09-07 18:02:45.960564	'mimimio':1	mimimio
78	channel	2025-09-07 18:03:36.797413	'mimimio':1	mimimio
79	channel	2025-09-07 18:06:22.852762		\N
80	channel	2025-09-07 18:09:12.73226	'mimimio':1	mimimio
81	channel	2025-09-07 18:09:39.430447	'mimimio':1	mimimio
82	channel	2025-09-07 18:11:27.700007	'mimimio':1	mimimio
83	channel	2025-09-07 18:12:05.358657	'mimimio':1	mimimio
84	private	2025-09-07 19:42:57.000551		\N
85	private	2025-09-07 19:43:04.476471		\N
25	private	2025-09-05 22:04:12.160749	'mobin':1	mobin
26	private	2025-09-05 22:04:13.847001	'ali':1	ali
27	private	2025-09-05 22:04:16.805964	'amir':1	amir
46	group	2025-09-06 00:23:09.470953	'dev':2 'group':3 'mobin':1	Mobin Dev Group
62	group	2025-09-06 02:08:18.037246	'momo':1	momo
\.


--
-- TOC entry 5074 (class 0 OID 16788)
-- Dependencies: 230
-- Data for Name: contacts; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.contacts (owner_id, contact_id, added_at) FROM stdin;
\.


--
-- TOC entry 5068 (class 0 OID 16644)
-- Dependencies: 224
-- Data for Name: group_chat; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.group_chat (chat_id, group_name, description, creator_id, created_at, is_private, group_avatar_url, search_vector) FROM stdin;
46	Mobin Dev Group	گروه تستی برای بررسی عملکرد سرچ	12	2025-09-06 00:23:09.489483	f	\N	'dev':2 'group':3 'mobin':1
72	momoo	yayay hoihihi	12	2025-09-07 17:49:51.765324	f	\N	\N
\.


--
-- TOC entry 5069 (class 0 OID 16671)
-- Dependencies: 225
-- Data for Name: group_members; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.group_members (chat_id, user_id, role, joined_at, invited_by) FROM stdin;
72	12	member	2025-09-07 17:49:51.80175	\N
\.


--
-- TOC entry 5073 (class 0 OID 16738)
-- Dependencies: 229
-- Data for Name: messages; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.messages (message_id, chat_id, sender_id, message_text, message_type, reply_to_message_id, forwarded_from_message_id, is_edited, is_deleted, status, sent_at, search_vector) FROM stdin;
15	27	12	hihi	text	\N	\N	f	f	sent	2025-09-05 22:04:19.053841+03:30	'hihi':1
16	27	12	hi	text	\N	\N	f	f	sent	2025-09-05 22:21:35.669178+03:30	'hi':1
17	26	12	hi	text	\N	\N	f	f	sent	2025-09-05 22:24:17.510649+03:30	'hi':1
18	25	12	hi	text	\N	\N	f	f	sent	2025-09-05 22:26:27.872925+03:30	'hi':1
19	62	12	hi	text	\N	\N	f	f	sent	2025-09-06 02:34:28.462324+03:30	'hi':1
20	25	12	hi	text	\N	\N	f	f	sent	2025-09-06 02:40:44.861226+03:30	'hi':1
21	25	12	hi	text	\N	\N	f	f	sent	2025-09-06 02:40:56.302001+03:30	'hi':1
22	25	12	hi	text	\N	\N	f	f	sent	2025-09-06 03:50:14.310743+03:30	'hi':1
23	46	12	hi	text	\N	\N	f	f	sent	2025-09-06 03:58:09.279645+03:30	'hi':1
24	46	12	gavv	text	\N	\N	f	f	sent	2025-09-06 03:58:23.011173+03:30	'gavv':1
25	46	12	mobin	text	\N	\N	f	f	sent	2025-09-06 04:35:51.75097+03:30	'mobin':1
26	25	12	hi	text	\N	\N	f	f	sent	2025-09-06 11:45:01.6095+03:30	'hi':1
27	25	12	h	text	\N	\N	f	f	sent	2025-09-06 11:45:07.638651+03:30	'h':1
28	25	12	hi	text	\N	\N	f	f	sent	2025-09-06 12:14:46.794841+03:30	'hi':1
29	25	12	hi	text	\N	\N	f	f	sent	2025-09-06 12:15:20.594445+03:30	'hi':1
30	26	12	hi	text	\N	\N	f	f	sent	2025-09-06 12:15:29.643475+03:30	'hi':1
31	27	12	haji pashmam	text	\N	\N	f	f	sent	2025-09-06 12:39:20.152915+03:30	'haji':1 'pashmam':2
32	25	12	hi	text	\N	\N	f	f	sent	2025-09-06 12:45:28.07383+03:30	'hi':1
33	25	12	اهbox boz	text	\N	\N	f	f	sent	2025-09-06 13:05:39.683571+03:30	'boz':2 'اهbox':1
34	25	12	hi	text	\N	\N	f	f	sent	2025-09-06 13:05:44.542185+03:30	'hi':1
35	25	12	sj	text	\N	\N	f	f	sent	2025-09-06 13:05:49.179677+03:30	'sj':1
36	25	12	سلام احمق	text	\N	\N	f	f	sent	2025-09-06 13:05:53.74412+03:30	'احمق':2 'سلام':1
37	46	12	fgh	text	\N	\N	f	f	sent	2025-09-06 13:32:30.443942+03:30	'fgh':1
38	27	12	اه	text	\N	\N	f	f	sent	2025-09-06 23:20:47.295084+03:30	'اه':1
45	83	12	fb	text	\N	\N	f	f	sent	2025-09-07 18:22:35.563046+03:30	'fb':1
46	72	12	hi	text	\N	\N	f	f	sent	2025-09-07 18:34:24.114582+03:30	'hi':1
47	25	12	hi	text	\N	\N	f	f	sent	2025-09-07 19:03:58.459458+03:30	'hi':1
48	84	16	hi	text	\N	\N	f	f	sent	2025-09-07 19:43:00.133965+03:30	'hi':1
49	25	12	hi	text	\N	\N	f	f	sent	2025-09-07 21:30:44.949158+03:30	'hi':1
50	27	12	hi\\	text	\N	\N	f	f	sent	2025-09-07 21:33:07.558381+03:30	'hi':1
51	25	12	xv	text	\N	\N	f	f	sent	2025-09-08 01:26:35.62866+03:30	'xv':1
52	25	12	zxc	text	\N	\N	f	f	sent	2025-09-08 01:26:43.18178+03:30	'zxc':1
53	25	12	hi	text	\N	\N	f	f	sent	2025-09-08 07:54:11.370391+03:30	'hi':1
54	26	12	mo	text	\N	\N	f	f	sent	2025-09-08 07:54:17.000606+03:30	'mo':1
\.


--
-- TOC entry 5076 (class 0 OID 24976)
-- Dependencies: 232
-- Data for Name: private_chat; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.private_chat (chat_id, user1_id, user2_id) FROM stdin;
25	12	12
26	12	13
27	12	15
84	12	16
85	13	16
\.


--
-- TOC entry 5065 (class 0 OID 16596)
-- Dependencies: 221
-- Data for Name: user_profiles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_profiles (profile_id, user_id, profile_image_url, bio, updated_at, username, password, profile_name, search_vector) FROM stdin;
1	5	\N	\N	2025-08-13 01:23:42.354081	\N	\N	username	'username':1B
2	6	\N	\N	2025-09-03 02:08:54.852628	\N	\N	username	'username':1B
8	12	\N	mori zer nazan	2025-09-03 19:36:22.431	fermow	\N	mobin	'fermow':1A 'mobin':2B 'mori':3C 'nazan':5C 'zer':4C
9	13	\N	hoho	2025-09-03 23:49:25.437	seyed	\N	ali	'ali':2B 'hoho':3C 'seyed':1A
11	15	\N	gogoli	2025-09-04 21:37:26.306	mesterii	\N	amir	'amir':2 'mesterii':1
12	16	\N	nabayad zer bezanam	2025-09-07 19:42:48.176	mori	\N	morteza	'mori':1 'morteza':2
\.


--
-- TOC entry 5063 (class 0 OID 16560)
-- Dependencies: 219
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (user_id, created_at, last_seen, is_verified, is_online, is_deleted, email) FROM stdin;
3	2025-07-24 12:31:54.952129+03:30	\N	t	f	f	mobin@email
5	2025-08-13 01:23:42.348183+03:30	\N	f	f	f	amir@email
6	2025-09-03 02:08:54.845412+03:30	\N	f	f	f	mobin@example.com
12	2025-09-03 19:36:08.125112+03:30	\N	t	f	f	fermow.fermow85@gmail.com
15	2025-09-04 21:36:26.543533+03:30	\N	t	f	f	mesterdidi13831383@gmail.com
13	2025-09-03 23:49:18.340947+03:30	\N	t	f	f	batman132ali@gmail.com
16	2025-09-07 19:42:26.089697+03:30	\N	t	f	f	azetrom.121@gmail.com
\.


--
-- TOC entry 5088 (class 0 OID 0)
-- Dependencies: 222
-- Name: chat_chat_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.chat_chat_id_seq', 85, true);


--
-- TOC entry 5089 (class 0 OID 0)
-- Dependencies: 228
-- Name: messages_message_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.messages_message_id_seq', 54, true);


--
-- TOC entry 5090 (class 0 OID 0)
-- Dependencies: 231
-- Name: private_chat_chat_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.private_chat_chat_id_seq', 11, true);


--
-- TOC entry 5091 (class 0 OID 0)
-- Dependencies: 220
-- Name: user_profiles_profile_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.user_profiles_profile_id_seq', 12, true);


--
-- TOC entry 5092 (class 0 OID 0)
-- Dependencies: 218
-- Name: users_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_user_id_seq', 16, true);


--
-- TOC entry 4886 (class 2606 OID 16701)
-- Name: channel channel_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.channel
    ADD CONSTRAINT channel_pkey PRIMARY KEY (chat_id);


--
-- TOC entry 4889 (class 2606 OID 16719)
-- Name: channel_subscribers channel_subscribers_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.channel_subscribers
    ADD CONSTRAINT channel_subscribers_pkey PRIMARY KEY (chat_id, user_id);


--
-- TOC entry 4878 (class 2606 OID 16621)
-- Name: chat chat_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat
    ADD CONSTRAINT chat_pkey PRIMARY KEY (chat_id);


--
-- TOC entry 4894 (class 2606 OID 16793)
-- Name: contacts contacts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contacts
    ADD CONSTRAINT contacts_pkey PRIMARY KEY (owner_id, contact_id);


--
-- TOC entry 4881 (class 2606 OID 16652)
-- Name: group_chat group_chat_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_chat
    ADD CONSTRAINT group_chat_pkey PRIMARY KEY (chat_id);


--
-- TOC entry 4884 (class 2606 OID 16677)
-- Name: group_members group_members_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_members
    ADD CONSTRAINT group_members_pkey PRIMARY KEY (chat_id, user_id);


--
-- TOC entry 4892 (class 2606 OID 16751)
-- Name: messages messages_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_pkey PRIMARY KEY (message_id);


--
-- TOC entry 4896 (class 2606 OID 24981)
-- Name: private_chat private_chat_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.private_chat
    ADD CONSTRAINT private_chat_pkey PRIMARY KEY (chat_id);


--
-- TOC entry 4876 (class 2606 OID 16606)
-- Name: user_profiles user_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_pkey PRIMARY KEY (profile_id);


--
-- TOC entry 4871 (class 2606 OID 16571)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);


--
-- TOC entry 4887 (class 1259 OID 25007)
-- Name: idx_channel_search_vector; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_channel_search_vector ON public.channel USING gin (search_vector);


--
-- TOC entry 4879 (class 1259 OID 25010)
-- Name: idx_chat_search_vector; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_chat_search_vector ON public.chat USING gin (search_vector);


--
-- TOC entry 4882 (class 1259 OID 25006)
-- Name: idx_group_search_vector; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_group_search_vector ON public.group_chat USING gin (search_vector);


--
-- TOC entry 4890 (class 1259 OID 16779)
-- Name: idx_messages_search_vector; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_messages_search_vector ON public.messages USING gin (search_vector);


--
-- TOC entry 4872 (class 1259 OID 16773)
-- Name: idx_user_profiles_search_vector; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_profiles_search_vector ON public.user_profiles USING gin (search_vector);


--
-- TOC entry 4873 (class 1259 OID 25093)
-- Name: idx_users_profile_name_trgm; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_profile_name_trgm ON public.user_profiles USING gin (profile_name public.gin_trgm_ops);


--
-- TOC entry 4874 (class 1259 OID 25092)
-- Name: idx_users_username_trgm; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_username_trgm ON public.user_profiles USING gin (username public.gin_trgm_ops);


--
-- TOC entry 4914 (class 2620 OID 24997)
-- Name: chat chat_search_update; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER chat_search_update BEFORE INSERT OR UPDATE ON public.chat FOR EACH ROW EXECUTE FUNCTION public.chat_search_update();


--
-- TOC entry 4915 (class 2620 OID 25009)
-- Name: chat trg_set_chat_name; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_set_chat_name BEFORE INSERT OR UPDATE ON public.chat FOR EACH ROW EXECUTE FUNCTION public.set_chat_name();


--
-- TOC entry 4913 (class 2620 OID 16772)
-- Name: user_profiles tsvectorupdate; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER tsvectorupdate BEFORE INSERT OR UPDATE ON public.user_profiles FOR EACH ROW EXECUTE FUNCTION tsvector_update_trigger('search_vector', 'pg_catalog.simple', 'username', 'profile_name');


--
-- TOC entry 4916 (class 2620 OID 16781)
-- Name: messages tsvectorupdate_messages; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER tsvectorupdate_messages BEFORE INSERT OR UPDATE ON public.messages FOR EACH ROW EXECUTE FUNCTION public.messages_tsvector_trigger();


--
-- TOC entry 4903 (class 2606 OID 16702)
-- Name: channel channel_chat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.channel
    ADD CONSTRAINT channel_chat_id_fkey FOREIGN KEY (chat_id) REFERENCES public.chat(chat_id) ON DELETE CASCADE;


--
-- TOC entry 4904 (class 2606 OID 16707)
-- Name: channel channel_owner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.channel
    ADD CONSTRAINT channel_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES public.users(user_id);


--
-- TOC entry 4905 (class 2606 OID 16720)
-- Name: channel_subscribers channel_subscribers_chat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.channel_subscribers
    ADD CONSTRAINT channel_subscribers_chat_id_fkey FOREIGN KEY (chat_id) REFERENCES public.channel(chat_id) ON DELETE CASCADE;


--
-- TOC entry 4906 (class 2606 OID 16725)
-- Name: channel_subscribers channel_subscribers_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.channel_subscribers
    ADD CONSTRAINT channel_subscribers_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- TOC entry 4911 (class 2606 OID 16799)
-- Name: contacts contacts_contact_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contacts
    ADD CONSTRAINT contacts_contact_id_fkey FOREIGN KEY (contact_id) REFERENCES public.users(user_id);


--
-- TOC entry 4912 (class 2606 OID 16794)
-- Name: contacts contacts_owner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contacts
    ADD CONSTRAINT contacts_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES public.users(user_id);


--
-- TOC entry 4898 (class 2606 OID 16653)
-- Name: group_chat group_chat_chat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_chat
    ADD CONSTRAINT group_chat_chat_id_fkey FOREIGN KEY (chat_id) REFERENCES public.chat(chat_id) ON DELETE CASCADE;


--
-- TOC entry 4899 (class 2606 OID 16658)
-- Name: group_chat group_chat_creator_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_chat
    ADD CONSTRAINT group_chat_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES public.users(user_id);


--
-- TOC entry 4900 (class 2606 OID 16678)
-- Name: group_members group_members_chat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_members
    ADD CONSTRAINT group_members_chat_id_fkey FOREIGN KEY (chat_id) REFERENCES public.group_chat(chat_id) ON DELETE CASCADE;


--
-- TOC entry 4901 (class 2606 OID 16688)
-- Name: group_members group_members_invited_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_members
    ADD CONSTRAINT group_members_invited_by_fkey FOREIGN KEY (invited_by) REFERENCES public.users(user_id);


--
-- TOC entry 4902 (class 2606 OID 16683)
-- Name: group_members group_members_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_members
    ADD CONSTRAINT group_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- TOC entry 4907 (class 2606 OID 16752)
-- Name: messages messages_chat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_chat_id_fkey FOREIGN KEY (chat_id) REFERENCES public.chat(chat_id) ON DELETE CASCADE;


--
-- TOC entry 4908 (class 2606 OID 16767)
-- Name: messages messages_forwarded_from_message_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_forwarded_from_message_id_fkey FOREIGN KEY (forwarded_from_message_id) REFERENCES public.messages(message_id);


--
-- TOC entry 4909 (class 2606 OID 16762)
-- Name: messages messages_reply_to_message_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_reply_to_message_id_fkey FOREIGN KEY (reply_to_message_id) REFERENCES public.messages(message_id);


--
-- TOC entry 4910 (class 2606 OID 16757)
-- Name: messages messages_sender_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(user_id);


--
-- TOC entry 4897 (class 2606 OID 16607)
-- Name: user_profiles user_profiles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


-- Completed on 2025-09-08 10:49:25

--
-- PostgreSQL database dump complete
--

