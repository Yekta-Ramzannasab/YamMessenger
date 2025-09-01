--
-- PostgreSQL database dump
--

-- Dumped from database version 17.5
-- Dumped by pg_dump version 17.5

-- Started on 2025-09-02 01:58:03

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
-- TOC entry 887 (class 1247 OID 16731)
-- Name: message_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.message_status AS ENUM (
    'sent',
    'delivered',
    'read'
);


ALTER TYPE public.message_status OWNER TO postgres;

--
-- TOC entry 875 (class 1247 OID 16664)
-- Name: role_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.role_type AS ENUM (
    'member',
    'admin',
    'owner'
);


ALTER TYPE public.role_type OWNER TO postgres;

--
-- TOC entry 231 (class 1255 OID 16780)
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
    is_private boolean DEFAULT false
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
-- TOC entry 222 (class 1259 OID 16613)
-- Name: chat; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.chat (
    chat_id bigint NOT NULL,
    chat_type character varying(20) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    search_vector tsvector,
    CONSTRAINT chat_chat_type_check CHECK (((chat_type)::text = ANY ((ARRAY['private'::character varying, 'group'::character varying, 'channel'::character varying])::text[])))
);


ALTER TABLE public.chat OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 16612)
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
-- TOC entry 5024 (class 0 OID 0)
-- Dependencies: 221
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
    is_private boolean DEFAULT false
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
-- TOC entry 5025 (class 0 OID 0)
-- Dependencies: 228
-- Name: messages_message_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.messages_message_id_seq OWNED BY public.messages.message_id;


--
-- TOC entry 223 (class 1259 OID 16622)
-- Name: private_chat; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.private_chat (
    chat_id bigint NOT NULL,
    user1_id bigint NOT NULL,
    user2_id bigint NOT NULL,
    CONSTRAINT no_self_chat CHECK ((user1_id <> user2_id))
);


ALTER TABLE public.private_chat OWNER TO postgres;

--
-- TOC entry 220 (class 1259 OID 16596)
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
    profile_name character varying(25) NOT NULL,
    search_vector tsvector
);


ALTER TABLE public.user_profiles OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 16595)
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
-- TOC entry 5026 (class 0 OID 0)
-- Dependencies: 219
-- Name: user_profiles_profile_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_profiles_profile_id_seq OWNED BY public.user_profiles.profile_id;


--
-- TOC entry 218 (class 1259 OID 16560)
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
-- TOC entry 217 (class 1259 OID 16559)
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
-- TOC entry 5027 (class 0 OID 0)
-- Dependencies: 217
-- Name: users_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.users_user_id_seq OWNED BY public.users.user_id;


--
-- TOC entry 4795 (class 2604 OID 16616)
-- Name: chat chat_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat ALTER COLUMN chat_id SET DEFAULT nextval('public.chat_chat_id_seq'::regclass);


--
-- TOC entry 4805 (class 2604 OID 16741)
-- Name: messages message_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages ALTER COLUMN message_id SET DEFAULT nextval('public.messages_message_id_seq'::regclass);


--
-- TOC entry 4793 (class 2604 OID 16599)
-- Name: user_profiles profile_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles ALTER COLUMN profile_id SET DEFAULT nextval('public.user_profiles_profile_id_seq'::regclass);


--
-- TOC entry 4788 (class 2604 OID 16563)
-- Name: users user_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users ALTER COLUMN user_id SET DEFAULT nextval('public.users_user_id_seq'::regclass);


--
-- TOC entry 5014 (class 0 OID 16693)
-- Dependencies: 226
-- Data for Name: channel; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.channel (chat_id, channel_name, description, owner_id, is_private) FROM stdin;
\.


--
-- TOC entry 5015 (class 0 OID 16712)
-- Dependencies: 227
-- Data for Name: channel_subscribers; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.channel_subscribers (chat_id, user_id, role, joined_at, approved) FROM stdin;
\.


--
-- TOC entry 5010 (class 0 OID 16613)
-- Dependencies: 222
-- Data for Name: chat; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.chat (chat_id, chat_type, created_at, search_vector) FROM stdin;
\.


--
-- TOC entry 5018 (class 0 OID 16788)
-- Dependencies: 230
-- Data for Name: contacts; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.contacts (owner_id, contact_id, added_at) FROM stdin;
\.


--
-- TOC entry 5012 (class 0 OID 16644)
-- Dependencies: 224
-- Data for Name: group_chat; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.group_chat (chat_id, group_name, description, creator_id, created_at, is_private) FROM stdin;
\.


--
-- TOC entry 5013 (class 0 OID 16671)
-- Dependencies: 225
-- Data for Name: group_members; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.group_members (chat_id, user_id, role, joined_at, invited_by) FROM stdin;
\.


--
-- TOC entry 5017 (class 0 OID 16738)
-- Dependencies: 229
-- Data for Name: messages; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.messages (message_id, chat_id, sender_id, message_text, message_type, reply_to_message_id, forwarded_from_message_id, is_edited, is_deleted, status, sent_at, search_vector) FROM stdin;
\.


--
-- TOC entry 5011 (class 0 OID 16622)
-- Dependencies: 223
-- Data for Name: private_chat; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.private_chat (chat_id, user1_id, user2_id) FROM stdin;
\.


--
-- TOC entry 5008 (class 0 OID 16596)
-- Dependencies: 220
-- Data for Name: user_profiles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_profiles (profile_id, user_id, profile_image_url, bio, updated_at, username, password, profile_name, search_vector) FROM stdin;
1	5	\N	\N	2025-08-13 01:23:42.354081	\N	\N	username	'username':1
\.


--
-- TOC entry 5006 (class 0 OID 16560)
-- Dependencies: 218
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (user_id, created_at, last_seen, is_verified, is_online, is_deleted, email) FROM stdin;
3	2025-07-24 12:31:54.952129+03:30	\N	t	f	f	mobin@email
5	2025-08-13 01:23:42.348183+03:30	\N	f	f	f	amir@email
\.


--
-- TOC entry 5028 (class 0 OID 0)
-- Dependencies: 221
-- Name: chat_chat_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.chat_chat_id_seq', 1, false);


--
-- TOC entry 5029 (class 0 OID 0)
-- Dependencies: 228
-- Name: messages_message_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.messages_message_id_seq', 1, false);


--
-- TOC entry 5030 (class 0 OID 0)
-- Dependencies: 219
-- Name: user_profiles_profile_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.user_profiles_profile_id_seq', 1, true);


--
-- TOC entry 5031 (class 0 OID 0)
-- Dependencies: 217
-- Name: users_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_user_id_seq', 5, true);


--
-- TOC entry 4830 (class 2606 OID 16701)
-- Name: channel channel_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.channel
    ADD CONSTRAINT channel_pkey PRIMARY KEY (chat_id);


--
-- TOC entry 4832 (class 2606 OID 16719)
-- Name: channel_subscribers channel_subscribers_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.channel_subscribers
    ADD CONSTRAINT channel_subscribers_pkey PRIMARY KEY (chat_id, user_id);


--
-- TOC entry 4821 (class 2606 OID 16621)
-- Name: chat chat_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat
    ADD CONSTRAINT chat_pkey PRIMARY KEY (chat_id);


--
-- TOC entry 4837 (class 2606 OID 16793)
-- Name: contacts contacts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contacts
    ADD CONSTRAINT contacts_pkey PRIMARY KEY (owner_id, contact_id);


--
-- TOC entry 4826 (class 2606 OID 16652)
-- Name: group_chat group_chat_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_chat
    ADD CONSTRAINT group_chat_pkey PRIMARY KEY (chat_id);


--
-- TOC entry 4828 (class 2606 OID 16677)
-- Name: group_members group_members_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_members
    ADD CONSTRAINT group_members_pkey PRIMARY KEY (chat_id, user_id);


--
-- TOC entry 4835 (class 2606 OID 16751)
-- Name: messages messages_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_pkey PRIMARY KEY (message_id);


--
-- TOC entry 4823 (class 2606 OID 16627)
-- Name: private_chat private_chat_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.private_chat
    ADD CONSTRAINT private_chat_pkey PRIMARY KEY (chat_id);


--
-- TOC entry 4819 (class 2606 OID 16606)
-- Name: user_profiles user_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_pkey PRIMARY KEY (profile_id);


--
-- TOC entry 4816 (class 2606 OID 16571)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);


--
-- TOC entry 4833 (class 1259 OID 16779)
-- Name: idx_messages_search_vector; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_messages_search_vector ON public.messages USING gin (search_vector);


--
-- TOC entry 4817 (class 1259 OID 16773)
-- Name: idx_user_profiles_search_vector; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_profiles_search_vector ON public.user_profiles USING gin (search_vector);


--
-- TOC entry 4824 (class 1259 OID 16643)
-- Name: unique_pair_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX unique_pair_idx ON public.private_chat USING btree (LEAST(user1_id, user2_id), GREATEST(user1_id, user2_id));


--
-- TOC entry 4858 (class 2620 OID 16778)
-- Name: chat chat_search_update; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER chat_search_update BEFORE INSERT OR UPDATE ON public.chat FOR EACH ROW EXECUTE FUNCTION tsvector_update_trigger('search_vector', 'pg_catalog.english', 'description');


--
-- TOC entry 4857 (class 2620 OID 16772)
-- Name: user_profiles tsvectorupdate; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER tsvectorupdate BEFORE INSERT OR UPDATE ON public.user_profiles FOR EACH ROW EXECUTE FUNCTION tsvector_update_trigger('search_vector', 'pg_catalog.simple', 'username', 'profile_name');


--
-- TOC entry 4859 (class 2620 OID 16781)
-- Name: messages tsvectorupdate_messages; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER tsvectorupdate_messages BEFORE INSERT OR UPDATE ON public.messages FOR EACH ROW EXECUTE FUNCTION public.messages_tsvector_trigger();


--
-- TOC entry 4847 (class 2606 OID 16702)
-- Name: channel channel_chat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.channel
    ADD CONSTRAINT channel_chat_id_fkey FOREIGN KEY (chat_id) REFERENCES public.chat(chat_id) ON DELETE CASCADE;


--
-- TOC entry 4848 (class 2606 OID 16707)
-- Name: channel channel_owner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.channel
    ADD CONSTRAINT channel_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES public.users(user_id);


--
-- TOC entry 4849 (class 2606 OID 16720)
-- Name: channel_subscribers channel_subscribers_chat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.channel_subscribers
    ADD CONSTRAINT channel_subscribers_chat_id_fkey FOREIGN KEY (chat_id) REFERENCES public.channel(chat_id) ON DELETE CASCADE;


--
-- TOC entry 4850 (class 2606 OID 16725)
-- Name: channel_subscribers channel_subscribers_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.channel_subscribers
    ADD CONSTRAINT channel_subscribers_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- TOC entry 4855 (class 2606 OID 16799)
-- Name: contacts contacts_contact_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contacts
    ADD CONSTRAINT contacts_contact_id_fkey FOREIGN KEY (contact_id) REFERENCES public.users(user_id);


--
-- TOC entry 4856 (class 2606 OID 16794)
-- Name: contacts contacts_owner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contacts
    ADD CONSTRAINT contacts_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES public.users(user_id);


--
-- TOC entry 4842 (class 2606 OID 16653)
-- Name: group_chat group_chat_chat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_chat
    ADD CONSTRAINT group_chat_chat_id_fkey FOREIGN KEY (chat_id) REFERENCES public.chat(chat_id) ON DELETE CASCADE;


--
-- TOC entry 4843 (class 2606 OID 16658)
-- Name: group_chat group_chat_creator_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_chat
    ADD CONSTRAINT group_chat_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES public.users(user_id);


--
-- TOC entry 4844 (class 2606 OID 16678)
-- Name: group_members group_members_chat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_members
    ADD CONSTRAINT group_members_chat_id_fkey FOREIGN KEY (chat_id) REFERENCES public.group_chat(chat_id) ON DELETE CASCADE;


--
-- TOC entry 4845 (class 2606 OID 16688)
-- Name: group_members group_members_invited_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_members
    ADD CONSTRAINT group_members_invited_by_fkey FOREIGN KEY (invited_by) REFERENCES public.users(user_id);


--
-- TOC entry 4846 (class 2606 OID 16683)
-- Name: group_members group_members_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_members
    ADD CONSTRAINT group_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- TOC entry 4851 (class 2606 OID 16752)
-- Name: messages messages_chat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_chat_id_fkey FOREIGN KEY (chat_id) REFERENCES public.chat(chat_id) ON DELETE CASCADE;


--
-- TOC entry 4852 (class 2606 OID 16767)
-- Name: messages messages_forwarded_from_message_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_forwarded_from_message_id_fkey FOREIGN KEY (forwarded_from_message_id) REFERENCES public.messages(message_id);


--
-- TOC entry 4853 (class 2606 OID 16762)
-- Name: messages messages_reply_to_message_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_reply_to_message_id_fkey FOREIGN KEY (reply_to_message_id) REFERENCES public.messages(message_id);


--
-- TOC entry 4854 (class 2606 OID 16757)
-- Name: messages messages_sender_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(user_id);


--
-- TOC entry 4839 (class 2606 OID 16628)
-- Name: private_chat private_chat_chat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.private_chat
    ADD CONSTRAINT private_chat_chat_id_fkey FOREIGN KEY (chat_id) REFERENCES public.chat(chat_id) ON DELETE CASCADE;


--
-- TOC entry 4840 (class 2606 OID 16633)
-- Name: private_chat private_chat_user1_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.private_chat
    ADD CONSTRAINT private_chat_user1_id_fkey FOREIGN KEY (user1_id) REFERENCES public.users(user_id);


--
-- TOC entry 4841 (class 2606 OID 16638)
-- Name: private_chat private_chat_user2_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.private_chat
    ADD CONSTRAINT private_chat_user2_id_fkey FOREIGN KEY (user2_id) REFERENCES public.users(user_id);


--
-- TOC entry 4838 (class 2606 OID 16607)
-- Name: user_profiles user_profiles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


-- Completed on 2025-09-02 01:58:03

--
-- PostgreSQL database dump complete
--

