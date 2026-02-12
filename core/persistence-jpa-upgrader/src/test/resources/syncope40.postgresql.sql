--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
-- 
--   http://www.apache.org/licenses/LICENSE-2.0
-- 
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

--
-- TOC entry 217 (class 1259 OID 16390)
-- Name: accesspolicy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.accesspolicy (
    id character varying(36) NOT NULL,
    name character varying(255),
    jsonconf text
);


--
-- TOC entry 218 (class 1259 OID 16397)
-- Name: accesstoken; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.accesstoken (
    id character varying(255) NOT NULL,
    authorities bytea,
    body text,
    expirationtime timestamp with time zone,
    owner character varying(255)
);


--
-- TOC entry 219 (class 1259 OID 16406)
-- Name: accountpolicy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.accountpolicy (
    id character varying(36) NOT NULL,
    name character varying(255),
    maxauthenticationattempts integer,
    propagatesuspension integer
);


--
-- TOC entry 220 (class 1259 OID 16411)
-- Name: accountpolicyrule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.accountpolicyrule (
    policy_id character varying(36),
    implementation_id character varying(255)
);


--
-- TOC entry 352 (class 1259 OID 18316)
-- Name: act_evt_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_evt_log (
    log_nr_ integer NOT NULL,
    type_ character varying(64),
    proc_def_id_ character varying(64),
    proc_inst_id_ character varying(64),
    execution_id_ character varying(64),
    task_id_ character varying(64),
    time_stamp_ timestamp without time zone NOT NULL,
    user_id_ character varying(255),
    data_ bytea,
    lock_owner_ character varying(255),
    lock_time_ timestamp without time zone,
    is_processed_ smallint DEFAULT 0
);


--
-- TOC entry 351 (class 1259 OID 18315)
-- Name: act_evt_log_log_nr__seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.act_evt_log_log_nr__seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4932 (class 0 OID 0)
-- Dependencies: 351
-- Name: act_evt_log_log_nr__seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.act_evt_log_log_nr__seq OWNED BY public.act_evt_log.log_nr_;


--
-- TOC entry 327 (class 1259 OID 18006)
-- Name: act_ge_bytearray; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_ge_bytearray (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    name_ character varying(255),
    deployment_id_ character varying(64),
    bytes_ bytea,
    generated_ boolean
);


--
-- TOC entry 326 (class 1259 OID 18001)
-- Name: act_ge_property; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_ge_property (
    name_ character varying(64) NOT NULL,
    value_ character varying(300),
    rev_ integer
);


--
-- TOC entry 356 (class 1259 OID 18550)
-- Name: act_hi_actinst; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_hi_actinst (
    id_ character varying(64) NOT NULL,
    rev_ integer DEFAULT 1,
    proc_def_id_ character varying(64) NOT NULL,
    proc_inst_id_ character varying(64) NOT NULL,
    execution_id_ character varying(64) NOT NULL,
    act_id_ character varying(255) NOT NULL,
    task_id_ character varying(64),
    call_proc_inst_id_ character varying(64),
    act_name_ character varying(255),
    act_type_ character varying(255) NOT NULL,
    assignee_ character varying(255),
    completed_by_ character varying(255),
    start_time_ timestamp without time zone NOT NULL,
    end_time_ timestamp without time zone,
    transaction_order_ integer,
    duration_ bigint,
    delete_reason_ character varying(4000),
    tenant_id_ character varying(255) DEFAULT ''::character varying
);


--
-- TOC entry 359 (class 1259 OID 18573)
-- Name: act_hi_attachment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_hi_attachment (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    user_id_ character varying(255),
    name_ character varying(255),
    description_ character varying(4000),
    type_ character varying(255),
    task_id_ character varying(64),
    proc_inst_id_ character varying(64),
    url_ character varying(4000),
    content_id_ character varying(64),
    time_ timestamp without time zone
);


--
-- TOC entry 358 (class 1259 OID 18566)
-- Name: act_hi_comment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_hi_comment (
    id_ character varying(64) NOT NULL,
    type_ character varying(255),
    time_ timestamp without time zone NOT NULL,
    user_id_ character varying(255),
    task_id_ character varying(64),
    proc_inst_id_ character varying(64),
    action_ character varying(255),
    message_ character varying(4000),
    full_msg_ bytea
);


--
-- TOC entry 357 (class 1259 OID 18559)
-- Name: act_hi_detail; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_hi_detail (
    id_ character varying(64) NOT NULL,
    type_ character varying(255) NOT NULL,
    proc_inst_id_ character varying(64),
    execution_id_ character varying(64),
    task_id_ character varying(64),
    act_inst_id_ character varying(64),
    name_ character varying(255) NOT NULL,
    var_type_ character varying(64),
    rev_ integer,
    time_ timestamp without time zone NOT NULL,
    bytearray_id_ character varying(64),
    double_ double precision,
    long_ bigint,
    text_ character varying(4000),
    text2_ character varying(4000)
);


--
-- TOC entry 329 (class 1259 OID 18024)
-- Name: act_hi_entitylink; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_hi_entitylink (
    id_ character varying(64) NOT NULL,
    link_type_ character varying(255),
    create_time_ timestamp without time zone,
    scope_id_ character varying(255),
    sub_scope_id_ character varying(255),
    scope_type_ character varying(255),
    scope_definition_id_ character varying(255),
    parent_element_id_ character varying(255),
    ref_scope_id_ character varying(255),
    ref_scope_type_ character varying(255),
    ref_scope_definition_id_ character varying(255),
    root_scope_id_ character varying(255),
    root_scope_type_ character varying(255),
    hierarchy_type_ character varying(255)
);


--
-- TOC entry 331 (class 1259 OID 18047)
-- Name: act_hi_identitylink; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_hi_identitylink (
    id_ character varying(64) NOT NULL,
    group_id_ character varying(255),
    type_ character varying(255),
    user_id_ character varying(255),
    task_id_ character varying(64),
    create_time_ timestamp without time zone,
    proc_inst_id_ character varying(64),
    scope_id_ character varying(255),
    sub_scope_id_ character varying(255),
    scope_type_ character varying(255),
    scope_definition_id_ character varying(255)
);


--
-- TOC entry 355 (class 1259 OID 18539)
-- Name: act_hi_procinst; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_hi_procinst (
    id_ character varying(64) NOT NULL,
    rev_ integer DEFAULT 1,
    proc_inst_id_ character varying(64) NOT NULL,
    business_key_ character varying(255),
    proc_def_id_ character varying(64) NOT NULL,
    start_time_ timestamp without time zone NOT NULL,
    end_time_ timestamp without time zone,
    duration_ bigint,
    start_user_id_ character varying(255),
    start_act_id_ character varying(255),
    end_act_id_ character varying(255),
    super_process_instance_id_ character varying(64),
    delete_reason_ character varying(4000),
    tenant_id_ character varying(255) DEFAULT ''::character varying,
    name_ character varying(255),
    callback_id_ character varying(255),
    callback_type_ character varying(255),
    reference_id_ character varying(255),
    reference_type_ character varying(255),
    propagated_stage_inst_id_ character varying(255),
    business_status_ character varying(255)
);


--
-- TOC entry 341 (class 1259 OID 18221)
-- Name: act_hi_taskinst; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_hi_taskinst (
    id_ character varying(64) NOT NULL,
    rev_ integer DEFAULT 1,
    proc_def_id_ character varying(64),
    task_def_id_ character varying(64),
    task_def_key_ character varying(255),
    proc_inst_id_ character varying(64),
    execution_id_ character varying(64),
    scope_id_ character varying(255),
    sub_scope_id_ character varying(255),
    scope_type_ character varying(255),
    scope_definition_id_ character varying(255),
    propagated_stage_inst_id_ character varying(255),
    state_ character varying(255),
    name_ character varying(255),
    parent_task_id_ character varying(64),
    description_ character varying(4000),
    owner_ character varying(255),
    assignee_ character varying(255),
    start_time_ timestamp without time zone NOT NULL,
    in_progress_time_ timestamp without time zone,
    in_progress_started_by_ character varying(255),
    claim_time_ timestamp without time zone,
    claimed_by_ character varying(255),
    suspended_time_ timestamp without time zone,
    suspended_by_ character varying(255),
    end_time_ timestamp without time zone,
    completed_by_ character varying(255),
    duration_ bigint,
    delete_reason_ character varying(4000),
    priority_ integer,
    in_progress_due_date_ timestamp without time zone,
    due_date_ timestamp without time zone,
    form_key_ character varying(255),
    category_ character varying(255),
    tenant_id_ character varying(255) DEFAULT ''::character varying,
    last_updated_time_ timestamp without time zone
);


--
-- TOC entry 343 (class 1259 OID 18231)
-- Name: act_hi_tsk_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_hi_tsk_log (
    id_ integer NOT NULL,
    type_ character varying(64),
    task_id_ character varying(64) NOT NULL,
    time_stamp_ timestamp without time zone NOT NULL,
    user_id_ character varying(255),
    data_ character varying(4000),
    execution_id_ character varying(64),
    proc_inst_id_ character varying(64),
    proc_def_id_ character varying(64),
    scope_id_ character varying(255),
    scope_definition_id_ character varying(255),
    sub_scope_id_ character varying(255),
    scope_type_ character varying(255),
    tenant_id_ character varying(255) DEFAULT ''::character varying
);


--
-- TOC entry 342 (class 1259 OID 18230)
-- Name: act_hi_tsk_log_id__seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.act_hi_tsk_log_id__seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4933 (class 0 OID 0)
-- Dependencies: 342
-- Name: act_hi_tsk_log_id__seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.act_hi_tsk_log_id__seq OWNED BY public.act_hi_tsk_log.id_;


--
-- TOC entry 345 (class 1259 OID 18259)
-- Name: act_hi_varinst; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_hi_varinst (
    id_ character varying(64) NOT NULL,
    rev_ integer DEFAULT 1,
    proc_inst_id_ character varying(64),
    execution_id_ character varying(64),
    task_id_ character varying(64),
    name_ character varying(255) NOT NULL,
    var_type_ character varying(100),
    scope_id_ character varying(255),
    sub_scope_id_ character varying(255),
    scope_type_ character varying(255),
    bytearray_id_ character varying(64),
    double_ double precision,
    long_ bigint,
    text_ character varying(4000),
    text2_ character varying(4000),
    meta_info_ character varying(4000),
    create_time_ timestamp without time zone,
    last_updated_time_ timestamp without time zone
);


--
-- TOC entry 361 (class 1259 OID 18603)
-- Name: act_id_bytearray; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_id_bytearray (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    name_ character varying(255),
    bytes_ bytea
);


--
-- TOC entry 362 (class 1259 OID 18610)
-- Name: act_id_group; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_id_group (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    name_ character varying(255),
    type_ character varying(255)
);


--
-- TOC entry 365 (class 1259 OID 18630)
-- Name: act_id_info; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_id_info (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    user_id_ character varying(64),
    type_ character varying(64),
    key_ character varying(255),
    value_ character varying(255),
    password_ bytea,
    parent_id_ character varying(255)
);


--
-- TOC entry 363 (class 1259 OID 18617)
-- Name: act_id_membership; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_id_membership (
    user_id_ character varying(64) NOT NULL,
    group_id_ character varying(64) NOT NULL
);


--
-- TOC entry 367 (class 1259 OID 18644)
-- Name: act_id_priv; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_id_priv (
    id_ character varying(64) NOT NULL,
    name_ character varying(255) NOT NULL
);


--
-- TOC entry 368 (class 1259 OID 18649)
-- Name: act_id_priv_mapping; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_id_priv_mapping (
    id_ character varying(64) NOT NULL,
    priv_id_ character varying(64) NOT NULL,
    user_id_ character varying(255),
    group_id_ character varying(255)
);


--
-- TOC entry 360 (class 1259 OID 18598)
-- Name: act_id_property; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_id_property (
    name_ character varying(64) NOT NULL,
    value_ character varying(300),
    rev_ integer
);


--
-- TOC entry 366 (class 1259 OID 18637)
-- Name: act_id_token; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_id_token (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    token_value_ character varying(255),
    token_date_ timestamp without time zone,
    ip_address_ character varying(255),
    user_agent_ character varying(255),
    user_id_ character varying(255),
    token_data_ character varying(2000)
);


--
-- TOC entry 364 (class 1259 OID 18622)
-- Name: act_id_user; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_id_user (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    first_ character varying(255),
    last_ character varying(255),
    display_name_ character varying(255),
    email_ character varying(255),
    pwd_ character varying(255),
    picture_id_ character varying(64),
    tenant_id_ character varying(255) DEFAULT ''::character varying
);


--
-- TOC entry 353 (class 1259 OID 18325)
-- Name: act_procdef_info; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_procdef_info (
    id_ character varying(64) NOT NULL,
    proc_def_id_ character varying(64) NOT NULL,
    rev_ integer,
    info_json_id_ character varying(64)
);


--
-- TOC entry 347 (class 1259 OID 18282)
-- Name: act_re_deployment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_re_deployment (
    id_ character varying(64) NOT NULL,
    name_ character varying(255),
    category_ character varying(255),
    key_ character varying(255),
    tenant_id_ character varying(255) DEFAULT ''::character varying,
    deploy_time_ timestamp without time zone,
    derived_from_ character varying(64),
    derived_from_root_ character varying(64),
    parent_deployment_id_ character varying(255),
    engine_version_ character varying(255)
);


--
-- TOC entry 348 (class 1259 OID 18290)
-- Name: act_re_model; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_re_model (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    name_ character varying(255),
    key_ character varying(255),
    category_ character varying(255),
    create_time_ timestamp without time zone,
    last_update_time_ timestamp without time zone,
    version_ integer,
    meta_info_ character varying(4000),
    deployment_id_ character varying(64),
    editor_source_value_id_ character varying(64),
    editor_source_extra_value_id_ character varying(64),
    tenant_id_ character varying(255) DEFAULT ''::character varying
);


--
-- TOC entry 350 (class 1259 OID 18306)
-- Name: act_re_procdef; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_re_procdef (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    category_ character varying(255),
    name_ character varying(255),
    key_ character varying(255) NOT NULL,
    version_ integer NOT NULL,
    deployment_id_ character varying(64),
    resource_name_ character varying(4000),
    dgrm_resource_name_ character varying(4000),
    description_ character varying(4000),
    has_start_form_key_ boolean,
    has_graphical_notation_ boolean,
    suspension_state_ integer,
    tenant_id_ character varying(255) DEFAULT ''::character varying,
    derived_from_ character varying(64),
    derived_from_root_ character varying(64),
    derived_version_ integer DEFAULT 0 NOT NULL,
    engine_version_ character varying(255)
);


--
-- TOC entry 354 (class 1259 OID 18330)
-- Name: act_ru_actinst; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_ru_actinst (
    id_ character varying(64) NOT NULL,
    rev_ integer DEFAULT 1,
    proc_def_id_ character varying(64) NOT NULL,
    proc_inst_id_ character varying(64) NOT NULL,
    execution_id_ character varying(64) NOT NULL,
    act_id_ character varying(255) NOT NULL,
    task_id_ character varying(64),
    call_proc_inst_id_ character varying(64),
    act_name_ character varying(255),
    act_type_ character varying(255) NOT NULL,
    assignee_ character varying(255),
    completed_by_ character varying(255),
    start_time_ timestamp without time zone NOT NULL,
    end_time_ timestamp without time zone,
    duration_ bigint,
    transaction_order_ integer,
    delete_reason_ character varying(4000),
    tenant_id_ character varying(255) DEFAULT ''::character varying
);


--
-- TOC entry 335 (class 1259 OID 18082)
-- Name: act_ru_deadletter_job; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_ru_deadletter_job (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    category_ character varying(255),
    type_ character varying(255) NOT NULL,
    exclusive_ boolean,
    execution_id_ character varying(64),
    process_instance_id_ character varying(64),
    proc_def_id_ character varying(64),
    element_id_ character varying(255),
    element_name_ character varying(255),
    scope_id_ character varying(255),
    sub_scope_id_ character varying(255),
    scope_type_ character varying(255),
    scope_definition_id_ character varying(255),
    correlation_id_ character varying(255),
    exception_stack_id_ character varying(64),
    exception_msg_ character varying(4000),
    duedate_ timestamp without time zone,
    repeat_ character varying(255),
    handler_type_ character varying(255),
    handler_cfg_ character varying(4000),
    custom_values_id_ character varying(64),
    create_time_ timestamp without time zone,
    tenant_id_ character varying(255) DEFAULT ''::character varying
);


--
-- TOC entry 328 (class 1259 OID 18013)
-- Name: act_ru_entitylink; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_ru_entitylink (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    create_time_ timestamp without time zone,
    link_type_ character varying(255),
    scope_id_ character varying(255),
    sub_scope_id_ character varying(255),
    scope_type_ character varying(255),
    scope_definition_id_ character varying(255),
    parent_element_id_ character varying(255),
    ref_scope_id_ character varying(255),
    ref_scope_type_ character varying(255),
    ref_scope_definition_id_ character varying(255),
    root_scope_id_ character varying(255),
    root_scope_type_ character varying(255),
    hierarchy_type_ character varying(255)
);


--
-- TOC entry 346 (class 1259 OID 18270)
-- Name: act_ru_event_subscr; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_ru_event_subscr (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    event_type_ character varying(255) NOT NULL,
    event_name_ character varying(255),
    execution_id_ character varying(64),
    proc_inst_id_ character varying(64),
    activity_id_ character varying(64),
    configuration_ character varying(255),
    created_ timestamp without time zone NOT NULL,
    proc_def_id_ character varying(64),
    sub_scope_id_ character varying(64),
    scope_id_ character varying(64),
    scope_definition_id_ character varying(64),
    scope_definition_key_ character varying(255),
    scope_type_ character varying(64),
    lock_time_ timestamp without time zone,
    lock_owner_ character varying(255),
    tenant_id_ character varying(255) DEFAULT ''::character varying
);


--
-- TOC entry 349 (class 1259 OID 18298)
-- Name: act_ru_execution; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_ru_execution (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    proc_inst_id_ character varying(64),
    business_key_ character varying(255),
    parent_id_ character varying(64),
    proc_def_id_ character varying(64),
    super_exec_ character varying(64),
    root_proc_inst_id_ character varying(64),
    act_id_ character varying(255),
    is_active_ boolean,
    is_concurrent_ boolean,
    is_scope_ boolean,
    is_event_scope_ boolean,
    is_mi_root_ boolean,
    suspension_state_ integer,
    cached_ent_state_ integer,
    tenant_id_ character varying(255) DEFAULT ''::character varying,
    name_ character varying(255),
    start_act_id_ character varying(255),
    start_time_ timestamp without time zone,
    start_user_id_ character varying(255),
    lock_time_ timestamp without time zone,
    lock_owner_ character varying(255),
    is_count_enabled_ boolean,
    evt_subscr_count_ integer,
    task_count_ integer,
    job_count_ integer,
    timer_job_count_ integer,
    susp_job_count_ integer,
    deadletter_job_count_ integer,
    external_worker_job_count_ integer,
    var_count_ integer,
    id_link_count_ integer,
    callback_id_ character varying(255),
    callback_type_ character varying(255),
    reference_id_ character varying(255),
    reference_type_ character varying(255),
    propagated_stage_inst_id_ character varying(255),
    business_status_ character varying(255)
);


--
-- TOC entry 337 (class 1259 OID 18098)
-- Name: act_ru_external_job; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_ru_external_job (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    category_ character varying(255),
    type_ character varying(255) NOT NULL,
    lock_exp_time_ timestamp without time zone,
    lock_owner_ character varying(255),
    exclusive_ boolean,
    execution_id_ character varying(64),
    process_instance_id_ character varying(64),
    proc_def_id_ character varying(64),
    element_id_ character varying(255),
    element_name_ character varying(255),
    scope_id_ character varying(255),
    sub_scope_id_ character varying(255),
    scope_type_ character varying(255),
    scope_definition_id_ character varying(255),
    correlation_id_ character varying(255),
    retries_ integer,
    exception_stack_id_ character varying(64),
    exception_msg_ character varying(4000),
    duedate_ timestamp without time zone,
    repeat_ character varying(255),
    handler_type_ character varying(255),
    handler_cfg_ character varying(4000),
    custom_values_id_ character varying(64),
    create_time_ timestamp without time zone,
    tenant_id_ character varying(255) DEFAULT ''::character varying
);


--
-- TOC entry 336 (class 1259 OID 18090)
-- Name: act_ru_history_job; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_ru_history_job (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    lock_exp_time_ timestamp without time zone,
    lock_owner_ character varying(255),
    retries_ integer,
    exception_stack_id_ character varying(64),
    exception_msg_ character varying(4000),
    handler_type_ character varying(255),
    handler_cfg_ character varying(4000),
    custom_values_id_ character varying(64),
    adv_handler_cfg_id_ character varying(64),
    create_time_ timestamp without time zone,
    scope_type_ character varying(255),
    tenant_id_ character varying(255) DEFAULT ''::character varying
);


--
-- TOC entry 330 (class 1259 OID 18035)
-- Name: act_ru_identitylink; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_ru_identitylink (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    group_id_ character varying(255),
    type_ character varying(255),
    user_id_ character varying(255),
    task_id_ character varying(64),
    proc_inst_id_ character varying(64),
    proc_def_id_ character varying(64),
    scope_id_ character varying(255),
    sub_scope_id_ character varying(255),
    scope_type_ character varying(255),
    scope_definition_id_ character varying(255)
);


--
-- TOC entry 332 (class 1259 OID 18058)
-- Name: act_ru_job; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_ru_job (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    category_ character varying(255),
    type_ character varying(255) NOT NULL,
    lock_exp_time_ timestamp without time zone,
    lock_owner_ character varying(255),
    exclusive_ boolean,
    execution_id_ character varying(64),
    process_instance_id_ character varying(64),
    proc_def_id_ character varying(64),
    element_id_ character varying(255),
    element_name_ character varying(255),
    scope_id_ character varying(255),
    sub_scope_id_ character varying(255),
    scope_type_ character varying(255),
    scope_definition_id_ character varying(255),
    correlation_id_ character varying(255),
    retries_ integer,
    exception_stack_id_ character varying(64),
    exception_msg_ character varying(4000),
    duedate_ timestamp without time zone,
    repeat_ character varying(255),
    handler_type_ character varying(255),
    handler_cfg_ character varying(4000),
    custom_values_id_ character varying(64),
    create_time_ timestamp without time zone,
    tenant_id_ character varying(255) DEFAULT ''::character varying
);


--
-- TOC entry 334 (class 1259 OID 18074)
-- Name: act_ru_suspended_job; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_ru_suspended_job (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    category_ character varying(255),
    type_ character varying(255) NOT NULL,
    exclusive_ boolean,
    execution_id_ character varying(64),
    process_instance_id_ character varying(64),
    proc_def_id_ character varying(64),
    element_id_ character varying(255),
    element_name_ character varying(255),
    scope_id_ character varying(255),
    sub_scope_id_ character varying(255),
    scope_type_ character varying(255),
    scope_definition_id_ character varying(255),
    correlation_id_ character varying(255),
    retries_ integer,
    exception_stack_id_ character varying(64),
    exception_msg_ character varying(4000),
    duedate_ timestamp without time zone,
    repeat_ character varying(255),
    handler_type_ character varying(255),
    handler_cfg_ character varying(4000),
    custom_values_id_ character varying(64),
    create_time_ timestamp without time zone,
    tenant_id_ character varying(255) DEFAULT ''::character varying
);


--
-- TOC entry 340 (class 1259 OID 18209)
-- Name: act_ru_task; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_ru_task (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    execution_id_ character varying(64),
    proc_inst_id_ character varying(64),
    proc_def_id_ character varying(64),
    task_def_id_ character varying(64),
    scope_id_ character varying(255),
    sub_scope_id_ character varying(255),
    scope_type_ character varying(255),
    scope_definition_id_ character varying(255),
    propagated_stage_inst_id_ character varying(255),
    state_ character varying(255),
    name_ character varying(255),
    parent_task_id_ character varying(64),
    description_ character varying(4000),
    task_def_key_ character varying(255),
    owner_ character varying(255),
    assignee_ character varying(255),
    delegation_ character varying(64),
    priority_ integer,
    create_time_ timestamp without time zone,
    in_progress_time_ timestamp without time zone,
    in_progress_started_by_ character varying(255),
    claim_time_ timestamp without time zone,
    claimed_by_ character varying(255),
    suspended_time_ timestamp without time zone,
    suspended_by_ character varying(255),
    in_progress_due_date_ timestamp without time zone,
    due_date_ timestamp without time zone,
    category_ character varying(255),
    suspension_state_ integer,
    tenant_id_ character varying(255) DEFAULT ''::character varying,
    form_key_ character varying(255),
    is_count_enabled_ boolean,
    var_count_ integer,
    id_link_count_ integer,
    sub_task_count_ integer
);


--
-- TOC entry 333 (class 1259 OID 18066)
-- Name: act_ru_timer_job; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_ru_timer_job (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    category_ character varying(255),
    type_ character varying(255) NOT NULL,
    lock_exp_time_ timestamp without time zone,
    lock_owner_ character varying(255),
    exclusive_ boolean,
    execution_id_ character varying(64),
    process_instance_id_ character varying(64),
    proc_def_id_ character varying(64),
    element_id_ character varying(255),
    element_name_ character varying(255),
    scope_id_ character varying(255),
    sub_scope_id_ character varying(255),
    scope_type_ character varying(255),
    scope_definition_id_ character varying(255),
    correlation_id_ character varying(255),
    retries_ integer,
    exception_stack_id_ character varying(64),
    exception_msg_ character varying(4000),
    duedate_ timestamp without time zone,
    repeat_ character varying(255),
    handler_type_ character varying(255),
    handler_cfg_ character varying(4000),
    custom_values_id_ character varying(64),
    create_time_ timestamp without time zone,
    tenant_id_ character varying(255) DEFAULT ''::character varying
);


--
-- TOC entry 344 (class 1259 OID 18244)
-- Name: act_ru_variable; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.act_ru_variable (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    type_ character varying(255) NOT NULL,
    name_ character varying(255) NOT NULL,
    execution_id_ character varying(64),
    proc_inst_id_ character varying(64),
    task_id_ character varying(64),
    scope_id_ character varying(255),
    sub_scope_id_ character varying(255),
    scope_type_ character varying(255),
    bytearray_id_ character varying(64),
    double_ double precision,
    long_ bigint,
    text_ character varying(4000),
    text2_ character varying(4000),
    meta_info_ character varying(4000)
);


--
-- TOC entry 369 (class 1259 OID 18681)
-- Name: adyngroupmembers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.adyngroupmembers (
    anytype_id character varying(255),
    any_id character(36),
    group_id character(36)
);


--
-- TOC entry 221 (class 1259 OID 16416)
-- Name: adyngroupmembership; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.adyngroupmembership (
    id character varying(36) NOT NULL,
    fiql character varying(255),
    group_id character varying(36),
    anytype_id character varying(255)
);


--
-- TOC entry 222 (class 1259 OID 16423)
-- Name: amembership; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.amembership (
    id character varying(36) NOT NULL,
    anyobject_id character varying(36),
    group_id character varying(36)
);


--
-- TOC entry 223 (class 1259 OID 16428)
-- Name: anyabout; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.anyabout (
    id character varying(36) NOT NULL,
    anytype_filter text,
    anytype_id character varying(255),
    notification_id character varying(36)
);


--
-- TOC entry 224 (class 1259 OID 16437)
-- Name: anyobject; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.anyobject (
    id character varying(36) NOT NULL,
    creationcontext character varying(255),
    creationdate timestamp with time zone,
    creator character varying(255),
    lastchangecontext character varying(255),
    lastchangedate timestamp with time zone,
    lastmodifier character varying(255),
    status character varying(255),
    name character varying(255),
    plainattrs jsonb,
    realm_id character varying(36),
    type_id character varying(255)
);


--
-- TOC entry 225 (class 1259 OID 16446)
-- Name: anyobject_anytypeclass; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.anyobject_anytypeclass (
    anyobject_id character varying(36),
    anytypeclass_id character varying(255)
);


--
-- TOC entry 226 (class 1259 OID 16451)
-- Name: anyobject_externalresource; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.anyobject_externalresource (
    anyobject_id character varying(36),
    resource_id character varying(255)
);


--
-- TOC entry 308 (class 1259 OID 17049)
-- Name: syncopegroup; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.syncopegroup (
    id character varying(36) NOT NULL,
    creationcontext character varying(255),
    creationdate timestamp with time zone,
    creator character varying(255),
    lastchangecontext character varying(255),
    lastchangedate timestamp with time zone,
    lastmodifier character varying(255),
    status character varying(255),
    name character varying(255),
    plainattrs jsonb,
    realm_id character varying(36),
    groupowner_id character varying(36),
    userowner_id character varying(36)
);


--
-- TOC entry 373 (class 1259 OID 18701)
-- Name: anyobject_search_amembership; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.anyobject_search_amembership AS
 SELECT m.anyobject_id AS any_id,
    g.id AS group_id,
    g.name AS group_name
   FROM public.amembership m,
    public.syncopegroup g
  WHERE ((m.group_id)::text = (g.id)::text);


--
-- TOC entry 233 (class 1259 OID 16498)
-- Name: arelationship; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arelationship (
    id character varying(36) NOT NULL,
    left_anyobject_id character varying(36),
    right_anyobject_id character varying(36),
    type_id character varying(255)
);


--
-- TOC entry 374 (class 1259 OID 18705)
-- Name: anyobject_search_arelationship; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.anyobject_search_arelationship AS
 SELECT left_anyobject_id AS any_id,
    right_anyobject_id AS right_any_id,
    type_id AS type
   FROM public.arelationship m;


--
-- TOC entry 375 (class 1259 OID 18709)
-- Name: anyobject_search_auxclass; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.anyobject_search_auxclass AS
 SELECT anyobject_id AS any_id,
    anytypeclass_id
   FROM public.anyobject_anytypeclass st;


--
-- TOC entry 310 (class 1259 OID 17063)
-- Name: syncopegroup_externalresource; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.syncopegroup_externalresource (
    group_id character varying(36),
    resource_id character varying(255)
);


--
-- TOC entry 376 (class 1259 OID 18713)
-- Name: anyobject_search_group_res; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.anyobject_search_group_res AS
 SELECT m.anyobject_id AS any_id,
    st.resource_id
   FROM public.amembership m,
    public.syncopegroup r,
    public.syncopegroup_externalresource st
  WHERE (((m.group_id)::text = (r.id)::text) AND ((st.group_id)::text = (r.id)::text));


--
-- TOC entry 377 (class 1259 OID 18717)
-- Name: anyobject_search_resource; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.anyobject_search_resource AS
 SELECT anyobject_id AS any_id,
    resource_id
   FROM public.anyobject_externalresource st;


--
-- TOC entry 227 (class 1259 OID 16456)
-- Name: anytemplatelivesynctask; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.anytemplatelivesynctask (
    id character varying(36) NOT NULL,
    template text,
    livesynctask_id character varying(36),
    anytype_id character varying(255)
);


--
-- TOC entry 228 (class 1259 OID 16465)
-- Name: anytemplatepulltask; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.anytemplatepulltask (
    id character varying(36) NOT NULL,
    template text,
    anytype_id character varying(255),
    pulltask_id character varying(36)
);


--
-- TOC entry 229 (class 1259 OID 16474)
-- Name: anytemplaterealm; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.anytemplaterealm (
    id character varying(36) NOT NULL,
    template text,
    realm_id character varying(36),
    anytype_id character varying(255)
);


--
-- TOC entry 230 (class 1259 OID 16483)
-- Name: anytype; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.anytype (
    id character varying(255) NOT NULL,
    kind character varying(20)
);


--
-- TOC entry 232 (class 1259 OID 16493)
-- Name: anytype_anytypeclass; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.anytype_anytypeclass (
    anytype_id character varying(255),
    anytypeclass_id character varying(255)
);


--
-- TOC entry 231 (class 1259 OID 16488)
-- Name: anytypeclass; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.anytypeclass (
    id character varying(255) NOT NULL
);


--
-- TOC entry 234 (class 1259 OID 16505)
-- Name: attrreleasepolicy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.attrreleasepolicy (
    id character varying(36) NOT NULL,
    name character varying(255),
    arporder integer,
    jsonconf text,
    status integer
);


--
-- TOC entry 235 (class 1259 OID 16512)
-- Name: attrrepo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.attrrepo (
    id character varying(255) NOT NULL,
    attrrepoorder integer,
    attrrepostate character varying(20),
    description character varying(255),
    items text,
    jsonconf text
);


--
-- TOC entry 236 (class 1259 OID 16519)
-- Name: auditconf; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auditconf (
    id character varying(255) NOT NULL,
    active integer
);


--
-- TOC entry 237 (class 1259 OID 16524)
-- Name: auditevent; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auditevent (
    id character varying(36) NOT NULL,
    before_value text,
    inputs text,
    opevent character varying(255),
    output text,
    throwable text,
    event_date timestamp with time zone NOT NULL,
    who character varying(255)
);


--
-- TOC entry 238 (class 1259 OID 16531)
-- Name: authmodule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.authmodule (
    id character varying(255) NOT NULL,
    authmoduleorder integer,
    authmodulestate character varying(20),
    description character varying(255),
    items text,
    jsonconf text
);


--
-- TOC entry 239 (class 1259 OID 16538)
-- Name: authpolicy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.authpolicy (
    id character varying(36) NOT NULL,
    name character varying(255),
    jsonconf text
);


--
-- TOC entry 240 (class 1259 OID 16545)
-- Name: authprofile; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.authprofile (
    id character varying(36) NOT NULL,
    googlemfaauthaccounts text,
    googlemfaauthtokens text,
    impersonationaccounts text,
    mfatrusteddevices text,
    owner character varying(255) NOT NULL,
    webauthndevicecredentials text
);


--
-- TOC entry 241 (class 1259 OID 16554)
-- Name: casspclientapp; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.casspclientapp (
    id character varying(36) NOT NULL,
    clientappid bigint NOT NULL,
    description character varying(255),
    evaluationorder integer,
    informationurl character varying(255),
    logo character varying(255),
    logouttype character varying(20),
    name character varying(255) NOT NULL,
    privacyurl character varying(255),
    properties text,
    theme character varying(255),
    usernameattributeproviderconf text,
    serviceid character varying(255) NOT NULL,
    accesspolicy_id character varying(36),
    attrreleasepolicy_id character varying(36),
    authpolicy_id character varying(36),
    realm_id character varying(36),
    ticketexpirationpolicy_id character varying(36)
);


--
-- TOC entry 242 (class 1259 OID 16567)
-- Name: confparam; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.confparam (
    id character varying(255) NOT NULL,
    jsonvalue text
);


--
-- TOC entry 243 (class 1259 OID 16574)
-- Name: conninstance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.conninstance (
    id character varying(36) NOT NULL,
    bundlename character varying(255),
    capabilities text,
    connrequesttimeout integer,
    connectorname character varying(255),
    displayname character varying(255),
    jsonconf text,
    location character varying(255),
    poolconf character varying(255),
    version character varying(255),
    adminrealm_id character varying(36)
);


--
-- TOC entry 244 (class 1259 OID 16583)
-- Name: delegation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.delegation (
    id character varying(36) NOT NULL,
    enddate timestamp with time zone,
    startdate timestamp with time zone NOT NULL,
    delegated_id character varying(36),
    delegating_id character varying(36)
);


--
-- TOC entry 245 (class 1259 OID 16590)
-- Name: delegation_syncoperole; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.delegation_syncoperole (
    jpadelegation_id character varying(36),
    roles_id character varying(255)
);


--
-- TOC entry 246 (class 1259 OID 16593)
-- Name: derschema; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.derschema (
    id character varying(255) NOT NULL,
    expression character varying(255),
    anytypeclass_id character varying(255)
);


--
-- TOC entry 247 (class 1259 OID 16600)
-- Name: dynrealm; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dynrealm (
    id character varying(255) NOT NULL
);


--
-- TOC entry 370 (class 1259 OID 18686)
-- Name: dynrealmmembers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dynrealmmembers (
    any_id character(36),
    dynrealm_id character varying(255)
);


--
-- TOC entry 248 (class 1259 OID 16605)
-- Name: dynrealmmembership; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dynrealmmembership (
    id character varying(36) NOT NULL,
    fiql character varying(255),
    dynrealm_id character varying(255),
    anytype_id character varying(255)
);


--
-- TOC entry 371 (class 1259 OID 18691)
-- Name: dynrolemembers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dynrolemembers (
    any_id character(36),
    role_id character varying(255)
);


--
-- TOC entry 249 (class 1259 OID 16612)
-- Name: externalresource; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.externalresource (
    id character varying(255) NOT NULL,
    capabilitiesoverride text,
    createtracelevel character varying(20),
    deletetracelevel character varying(20),
    enforcemandatorycondition integer,
    jsonconf text,
    orgunit text,
    propagationpriority integer,
    provisioningtracelevel character varying(20),
    provisions text,
    updatetracelevel character varying(20),
    accountpolicy_id character varying(36),
    connector_id character varying(36),
    inboundpolicy_id character varying(36),
    passwordpolicy_id character varying(36),
    propagationpolicy_id character varying(36),
    provisionsorter_id character varying(255),
    pushpolicy_id character varying(36)
);


--
-- TOC entry 250 (class 1259 OID 16619)
-- Name: externalresourcepropaction; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.externalresourcepropaction (
    resource_id character varying(255),
    implementation_id character varying(255)
);


--
-- TOC entry 251 (class 1259 OID 16626)
-- Name: fiqlquery; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fiqlquery (
    id character varying(36) NOT NULL,
    fiql character varying(255),
    name character varying(255),
    target character varying(255),
    owner_id character varying(36)
);


--
-- TOC entry 338 (class 1259 OID 18187)
-- Name: flw_ru_batch; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flw_ru_batch (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    type_ character varying(64) NOT NULL,
    search_key_ character varying(255),
    search_key2_ character varying(255),
    create_time_ timestamp without time zone NOT NULL,
    complete_time_ timestamp without time zone,
    status_ character varying(255),
    batch_doc_id_ character varying(64),
    tenant_id_ character varying(255) DEFAULT ''::character varying
);


--
-- TOC entry 339 (class 1259 OID 18195)
-- Name: flw_ru_batch_part; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flw_ru_batch_part (
    id_ character varying(64) NOT NULL,
    rev_ integer,
    batch_id_ character varying(64),
    type_ character varying(64) NOT NULL,
    scope_id_ character varying(64),
    sub_scope_id_ character varying(64),
    scope_type_ character varying(64),
    search_key_ character varying(255),
    search_key2_ character varying(255),
    create_time_ timestamp without time zone NOT NULL,
    complete_time_ timestamp without time zone,
    status_ character varying(255),
    result_doc_id_ character varying(64),
    tenant_id_ character varying(255) DEFAULT ''::character varying
);


--
-- TOC entry 252 (class 1259 OID 16635)
-- Name: formpropertydef; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.formpropertydef (
    id character varying(36) NOT NULL,
    datepattern character varying(255),
    dropdownfreeform integer,
    dropdownsingleselection integer,
    enumvalues text,
    idx integer,
    labels text,
    mimetype character varying(255),
    name character varying(255),
    readable integer,
    required integer,
    stringregex character varying(255),
    type character varying(20),
    writable integer,
    macrotask_id character varying(36)
);


--
-- TOC entry 253 (class 1259 OID 16642)
-- Name: grelationship; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.grelationship (
    id character varying(36) NOT NULL,
    group_id character varying(36),
    anyobject_id character varying(36),
    type_id character varying(255)
);


--
-- TOC entry 309 (class 1259 OID 17058)
-- Name: syncopegroup_anytypeclass; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.syncopegroup_anytypeclass (
    group_id character varying(36),
    anytypeclass_id character varying(255)
);


--
-- TOC entry 378 (class 1259 OID 18721)
-- Name: group_search_auxclass; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.group_search_auxclass AS
 SELECT group_id AS any_id,
    anytypeclass_id
   FROM public.syncopegroup_anytypeclass st;


--
-- TOC entry 379 (class 1259 OID 18725)
-- Name: group_search_grelationship; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.group_search_grelationship AS
 SELECT id AS any_id,
    anyobject_id AS right_any_id,
    type_id AS type
   FROM public.grelationship m;


--
-- TOC entry 380 (class 1259 OID 18729)
-- Name: group_search_resource; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.group_search_resource AS
 SELECT group_id AS any_id,
    resource_id
   FROM public.syncopegroup_externalresource st;


--
-- TOC entry 254 (class 1259 OID 16649)
-- Name: implementation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.implementation (
    id character varying(255) NOT NULL,
    body text,
    engine character varying(20) NOT NULL,
    type character varying(255) NOT NULL
);


--
-- TOC entry 255 (class 1259 OID 16656)
-- Name: inboundcorrelationruleentity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inboundcorrelationruleentity (
    id character varying(36) NOT NULL,
    inboundpolicy_id character varying(36),
    anytype_id character varying(255),
    implementation_id character varying(255)
);


--
-- TOC entry 256 (class 1259 OID 16665)
-- Name: inboundpolicy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inboundpolicy (
    id character varying(36) NOT NULL,
    name character varying(255),
    conflictresolutionaction character varying(20)
);


--
-- TOC entry 257 (class 1259 OID 16670)
-- Name: jobstatus; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.jobstatus (
    id character varying(255) NOT NULL,
    jobstatus character varying(255)
);


--
-- TOC entry 258 (class 1259 OID 16677)
-- Name: linkedaccount; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.linkedaccount (
    id character varying(36) NOT NULL,
    cipheralgorithm character varying(20),
    connobjectkeyvalue character varying(255),
    password character varying(255),
    plainattrs jsonb,
    suspended integer,
    username character varying(255),
    owner_id character varying(36),
    resource_id character varying(255)
);


--
-- TOC entry 259 (class 1259 OID 16686)
-- Name: livesynctask; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.livesynctask (
    id character varying(36) NOT NULL,
    active integer,
    cronexpression character varying(255),
    description character varying(255),
    name character varying(255) NOT NULL,
    concurrentsettings text,
    matchingrule character varying(20),
    performcreate integer,
    performdelete integer,
    performupdate integer,
    syncstatus integer,
    unmatchingrule character varying(20),
    remediation integer,
    delaysecondsacrossinvocations integer,
    jobdelegate_id character varying(255),
    resource_id character varying(255),
    destinationrealm_id character varying(36),
    livesyncdeltamapper_id character varying(255)
);


--
-- TOC entry 260 (class 1259 OID 16697)
-- Name: livesynctaskaction; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.livesynctaskaction (
    task_id character varying(36),
    implementation_id character varying(255)
);


--
-- TOC entry 261 (class 1259 OID 16702)
-- Name: livesynctaskexec; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.livesynctaskexec (
    id character varying(36) NOT NULL,
    enddate timestamp with time zone,
    executor character varying(255),
    message text,
    startdate timestamp with time zone NOT NULL,
    status character varying(255),
    task_id character varying(36)
);


--
-- TOC entry 262 (class 1259 OID 16709)
-- Name: macrotask; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.macrotask (
    id character varying(36) NOT NULL,
    active integer,
    cronexpression character varying(255),
    description character varying(255),
    name character varying(255) NOT NULL,
    continueonerror integer,
    saveexecs integer,
    jobdelegate_id character varying(255),
    macroactions_id character varying(255),
    realm_id character varying(36)
);


--
-- TOC entry 263 (class 1259 OID 16718)
-- Name: macrotaskcommand; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.macrotaskcommand (
    id character varying(36) NOT NULL,
    args text,
    idx integer,
    macrotask_id character varying(36),
    command_id character varying(255)
);


--
-- TOC entry 264 (class 1259 OID 16725)
-- Name: macrotaskexec; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.macrotaskexec (
    id character varying(36) NOT NULL,
    enddate timestamp with time zone,
    executor character varying(255),
    message text,
    startdate timestamp with time zone NOT NULL,
    status character varying(255),
    task_id character varying(36)
);


--
-- TOC entry 265 (class 1259 OID 16732)
-- Name: mailtemplate; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mailtemplate (
    id character varying(255) NOT NULL,
    htmltemplate text,
    texttemplate text
);


--
-- TOC entry 266 (class 1259 OID 16739)
-- Name: networkservice; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.networkservice (
    id character varying(36) NOT NULL,
    address character varying(255),
    type character varying(20)
);


--
-- TOC entry 267 (class 1259 OID 16746)
-- Name: notification; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notification (
    id character varying(36) NOT NULL,
    active integer,
    events text,
    recipientattrname character varying(255),
    recipientsfiql character varying(255),
    selfasrecipient integer,
    sender character varying(255),
    staticrecipients text,
    subject character varying(255),
    tracelevel character varying(20),
    recipientsprovider_id character varying(255),
    template_id character varying(255)
);


--
-- TOC entry 268 (class 1259 OID 16753)
-- Name: notificationtask; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notificationtask (
    id character varying(36) NOT NULL,
    anytypekind character varying(20),
    entitykey character varying(255),
    executed integer,
    htmlbody text,
    recipients text,
    sender character varying(255),
    subject character varying(255),
    textbody text,
    tracelevel character varying(20),
    notification_id character varying(36)
);


--
-- TOC entry 269 (class 1259 OID 16760)
-- Name: notificationtaskexec; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notificationtaskexec (
    id character varying(36) NOT NULL,
    enddate timestamp with time zone,
    executor character varying(255),
    message text,
    startdate timestamp with time zone NOT NULL,
    status character varying(255),
    task_id character varying(36)
);


--
-- TOC entry 270 (class 1259 OID 16767)
-- Name: oidcjwks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oidcjwks (
    id character varying(36) NOT NULL,
    "json" text NOT NULL
);


--
-- TOC entry 271 (class 1259 OID 16774)
-- Name: oidcprovider; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oidcprovider (
    id character varying(36) NOT NULL,
    authorizationendpoint character varying(255) NOT NULL,
    clientid character varying(255) NOT NULL,
    clientsecret character varying(255) NOT NULL,
    createunmatching integer,
    endsessionendpoint character varying(255),
    hasdiscovery boolean NOT NULL,
    issuer character varying(255) NOT NULL,
    items text,
    jwksuri character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    scopes character varying(255),
    selfregunmatching integer,
    tokenendpoint character varying(255) NOT NULL,
    updatematching integer,
    userinfoendpoint character varying(255)
);


--
-- TOC entry 272 (class 1259 OID 16787)
-- Name: oidcprovideraction; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oidcprovideraction (
    op_id character varying(36),
    implementation_id character varying(255)
);


--
-- TOC entry 273 (class 1259 OID 16790)
-- Name: oidcrpclientapp; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oidcrpclientapp (
    id character varying(36) NOT NULL,
    clientappid bigint NOT NULL,
    description character varying(255),
    evaluationorder integer,
    informationurl character varying(255),
    logo character varying(255),
    logouttype character varying(20),
    name character varying(255) NOT NULL,
    privacyurl character varying(255),
    properties text,
    theme character varying(255),
    usernameattributeproviderconf text,
    accesstokenmaxactivetokens bigint,
    accesstokenmaxtimetolive character varying(255),
    accesstokentimetokill character varying(255),
    applicationtype character varying(20),
    bypassapprovalprompt boolean,
    clientid character varying(255) NOT NULL,
    clientsecret character varying(255),
    devicetokentimetokill character varying(255),
    encryptidtoken boolean,
    generaterefreshtoken boolean,
    idtokenencryptionalg character varying(20),
    idtokenencryptionencoding character varying(20),
    idtokenissuer character varying(255),
    idtokensigningalg character varying(20),
    jwks text,
    jwksuri character varying(255),
    jwtaccesstoken boolean,
    logouturi character varying(255),
    redirecturis text,
    refreshtokenmaxactivetokens bigint,
    refreshtokentimetokill character varying(255),
    scopes text,
    signidtoken boolean,
    subjecttype character varying(20),
    supportedgranttypes text,
    supportedresponsetypes text,
    tokenendpointauthenticationmethod character varying(20),
    userinfoencryptedresponsealg character varying(20),
    userinfoencryptedresponseencoding character varying(20),
    userinfosigningalg character varying(20),
    accesspolicy_id character varying(36),
    attrreleasepolicy_id character varying(36),
    authpolicy_id character varying(36),
    realm_id character varying(36),
    ticketexpirationpolicy_id character varying(36)
);


--
-- TOC entry 274 (class 1259 OID 16803)
-- Name: oidcusertemplate; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oidcusertemplate (
    id character varying(36) NOT NULL,
    template text,
    op_id character varying(36),
    anytype_id character varying(255)
);


--
-- TOC entry 275 (class 1259 OID 16812)
-- Name: passwordpolicy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.passwordpolicy (
    id character varying(36) NOT NULL,
    name character varying(255),
    allownullpassword integer,
    historylength integer
);


--
-- TOC entry 276 (class 1259 OID 16817)
-- Name: passwordpolicyrule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.passwordpolicyrule (
    policy_id character varying(36),
    implementation_id character varying(255)
);


--
-- TOC entry 277 (class 1259 OID 16822)
-- Name: plainschema; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.plainschema (
    id character varying(255) NOT NULL,
    cipheralgorithm character varying(20),
    conversionpattern character varying(255),
    enumvalues text,
    mandatorycondition character varying(255),
    mimetype character varying(255),
    multivalue integer,
    readonly integer,
    secretkey character varying(255),
    type character varying(20),
    uniqueconstraint integer,
    anytypeclass_id character varying(255),
    dropdownvalueprovider_id character varying(255),
    validator_id character varying(255)
);


--
-- TOC entry 278 (class 1259 OID 16829)
-- Name: propagationpolicy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.propagationpolicy (
    id character varying(36) NOT NULL,
    name character varying(255),
    backoffparams character varying(255),
    backoffstrategy character varying(20),
    fetcharoundprovisioning integer,
    maxattempts integer,
    updatedelta integer
);


--
-- TOC entry 279 (class 1259 OID 16836)
-- Name: propagationtask; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.propagationtask (
    id character varying(36) NOT NULL,
    anytype character varying(255),
    anytypekind character varying(20),
    connobjectkey character varying(255),
    entitykey character varying(255),
    objectclassname character varying(255),
    oldconnobjectkey character varying(255),
    operation character varying(20),
    propagationdata text,
    resource_id character varying(255)
);


--
-- TOC entry 280 (class 1259 OID 16843)
-- Name: propagationtaskexec; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.propagationtaskexec (
    id character varying(36) NOT NULL,
    enddate timestamp with time zone,
    executor character varying(255),
    message text,
    startdate timestamp with time zone NOT NULL,
    status character varying(255),
    task_id character varying(36)
);


--
-- TOC entry 281 (class 1259 OID 16850)
-- Name: pulltask; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pulltask (
    id character varying(36) NOT NULL,
    active integer,
    cronexpression character varying(255),
    description character varying(255),
    name character varying(255) NOT NULL,
    concurrentsettings text,
    matchingrule character varying(20),
    performcreate integer,
    performdelete integer,
    performupdate integer,
    syncstatus integer,
    unmatchingrule character varying(20),
    remediation integer,
    pullmode character varying(23),
    jobdelegate_id character varying(255),
    resource_id character varying(255),
    destinationrealm_id character varying(36),
    reconfilterbuilder_id character varying(255)
);


--
-- TOC entry 282 (class 1259 OID 16859)
-- Name: pulltaskaction; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pulltaskaction (
    task_id character varying(36),
    implementation_id character varying(255)
);


--
-- TOC entry 283 (class 1259 OID 16864)
-- Name: pulltaskexec; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pulltaskexec (
    id character varying(36) NOT NULL,
    enddate timestamp with time zone,
    executor character varying(255),
    message text,
    startdate timestamp with time zone NOT NULL,
    status character varying(255),
    task_id character varying(36)
);


--
-- TOC entry 284 (class 1259 OID 16871)
-- Name: pushcorrelationruleentity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pushcorrelationruleentity (
    id character varying(36) NOT NULL,
    pushpolicy_id character varying(36),
    anytype_id character varying(255),
    implementation_id character varying(255)
);


--
-- TOC entry 285 (class 1259 OID 16880)
-- Name: pushpolicy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pushpolicy (
    id character varying(36) NOT NULL,
    name character varying(255),
    conflictresolutionaction character varying(20)
);


--
-- TOC entry 286 (class 1259 OID 16885)
-- Name: pushtask; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pushtask (
    id character varying(36) NOT NULL,
    active integer,
    cronexpression character varying(255),
    description character varying(255),
    name character varying(255) NOT NULL,
    concurrentsettings text,
    matchingrule character varying(20),
    performcreate integer,
    performdelete integer,
    performupdate integer,
    syncstatus integer,
    unmatchingrule character varying(20),
    filters text,
    jobdelegate_id character varying(255),
    resource_id character varying(255),
    sourcerealm_id character varying(36)
);


--
-- TOC entry 287 (class 1259 OID 16894)
-- Name: pushtaskaction; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pushtaskaction (
    task_id character varying(36),
    implementation_id character varying(255)
);


--
-- TOC entry 288 (class 1259 OID 16899)
-- Name: pushtaskexec; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pushtaskexec (
    id character varying(36) NOT NULL,
    enddate timestamp with time zone,
    executor character varying(255),
    message text,
    startdate timestamp with time zone NOT NULL,
    status character varying(255),
    task_id character varying(36)
);


--
-- TOC entry 289 (class 1259 OID 16906)
-- Name: realm; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.realm (
    id character varying(36) NOT NULL,
    fullpath character varying(255) NOT NULL,
    name character varying(255),
    plainattrs jsonb,
    accesspolicy_id character varying(36),
    accountpolicy_id character varying(36),
    attrreleasepolicy_id character varying(36),
    authpolicy_id character varying(36),
    parent_id character varying(36),
    passwordpolicy_id character varying(36),
    ticketexpirationpolicy_id character varying(36)
);


--
-- TOC entry 291 (class 1259 OID 16922)
-- Name: realm_anytypeclass; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.realm_anytypeclass (
    realm_id character varying(36),
    anytypeclass_id character varying(255)
);


--
-- TOC entry 292 (class 1259 OID 16925)
-- Name: realm_externalresource; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.realm_externalresource (
    realm_id character varying(36),
    resource_id character varying(255)
);


--
-- TOC entry 290 (class 1259 OID 16917)
-- Name: realmaction; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.realmaction (
    realm_id character varying(36),
    implementation_id character varying(255)
);


--
-- TOC entry 293 (class 1259 OID 16930)
-- Name: relationshiptype; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.relationshiptype (
    id character varying(255) NOT NULL,
    description character varying(255),
    leftendanytype_id character varying(255),
    rightendanytype_id character varying(255)
);


--
-- TOC entry 294 (class 1259 OID 16937)
-- Name: remediation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.remediation (
    id character varying(36) NOT NULL,
    error text,
    instant timestamp with time zone,
    operation character varying(20),
    payload text,
    remotename character varying(255),
    anytype_id character varying(255),
    pulltask_id character varying(36)
);


--
-- TOC entry 295 (class 1259 OID 16944)
-- Name: report; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.report (
    id character varying(36) NOT NULL,
    active integer,
    cronexpression character varying(255),
    fileext character varying(255),
    mimetype character varying(255),
    name character varying(255) NOT NULL,
    jobdelegate_id character varying(255)
);


--
-- TOC entry 296 (class 1259 OID 16953)
-- Name: reportexec; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reportexec (
    id character varying(36) NOT NULL,
    enddate timestamp with time zone,
    executor character varying(255),
    message text,
    startdate timestamp with time zone NOT NULL,
    status character varying(255),
    execresult bytea,
    report_id character varying(36)
);


--
-- TOC entry 297 (class 1259 OID 16960)
-- Name: saml2idp4uiaction; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.saml2idp4uiaction (
    saml2idp4ui_id character varying(36),
    implementation_id character varying(255)
);


--
-- TOC entry 298 (class 1259 OID 16963)
-- Name: saml2idpentity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.saml2idpentity (
    id character varying(255) NOT NULL,
    encryptioncertificate bytea,
    encryptionkey bytea,
    metadata bytea NOT NULL,
    signingcertificate bytea,
    signingkey bytea
);


--
-- TOC entry 299 (class 1259 OID 16970)
-- Name: saml2sp4uiidp; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.saml2sp4uiidp (
    id character varying(36) NOT NULL,
    bindingtype smallint NOT NULL,
    createunmatching integer,
    entityid character varying(255) NOT NULL,
    items text,
    logoutsupported integer,
    metadata bytea,
    name character varying(255) NOT NULL,
    selfregunmatching integer,
    updatematching integer,
    requestedauthncontextprovider_id character varying(255)
);


--
-- TOC entry 300 (class 1259 OID 16981)
-- Name: saml2sp4uiusertemplate; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.saml2sp4uiusertemplate (
    id character varying(36) NOT NULL,
    template text,
    idp_id character varying(36),
    anytype_id character varying(255)
);


--
-- TOC entry 301 (class 1259 OID 16990)
-- Name: saml2spclientapp; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.saml2spclientapp (
    id character varying(36) NOT NULL,
    clientappid bigint NOT NULL,
    description character varying(255),
    evaluationorder integer,
    informationurl character varying(255),
    logo character varying(255),
    logouttype character varying(20),
    name character varying(255) NOT NULL,
    privacyurl character varying(255),
    properties text,
    theme character varying(255),
    usernameattributeproviderconf text,
    assertionaudiences text,
    encryptassertions boolean,
    encblalg text,
    encdataalg text,
    enckeyalg text,
    encryptionoptional boolean,
    entityid character varying(255) NOT NULL,
    idp character varying(255),
    metadatalocation character varying(255) NOT NULL,
    metadatasignaturelocation character varying(255),
    nameidqualifier character varying(255),
    reqauthncontextclass character varying(255),
    requirednameidformat smallint,
    spnameidqualifier character varying(255),
    signassertions boolean,
    signresponses boolean,
    sigalgs text,
    sigblalg text,
    sigrefdigestmethod text,
    skewallowance integer,
    accesspolicy_id character varying(36),
    attrreleasepolicy_id character varying(36),
    authpolicy_id character varying(36),
    realm_id character varying(36),
    ticketexpirationpolicy_id character varying(36)
);


--
-- TOC entry 302 (class 1259 OID 17003)
-- Name: schedtask; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.schedtask (
    id character varying(36) NOT NULL,
    active integer,
    cronexpression character varying(255),
    description character varying(255),
    name character varying(255) NOT NULL,
    jobdelegate_id character varying(255)
);


--
-- TOC entry 303 (class 1259 OID 17012)
-- Name: schedtaskexec; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.schedtaskexec (
    id character varying(36) NOT NULL,
    enddate timestamp with time zone,
    executor character varying(255),
    message text,
    startdate timestamp with time zone NOT NULL,
    status character varying(255),
    task_id character varying(36)
);


--
-- TOC entry 304 (class 1259 OID 17019)
-- Name: securityquestion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.securityquestion (
    id character varying(36) NOT NULL,
    content character varying(255)
);


--
-- TOC entry 305 (class 1259 OID 17026)
-- Name: sraroute; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sraroute (
    id character varying(36) NOT NULL,
    csrf integer,
    error character varying(255),
    filters text,
    logout integer,
    name character varying(255) NOT NULL,
    postlogout character varying(255),
    predicates text,
    routeorder integer,
    routetype character varying(20),
    target character varying(255)
);


--
-- TOC entry 306 (class 1259 OID 17035)
-- Name: syncopebatch; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.syncopebatch (
    id character varying(255) NOT NULL,
    expirytime timestamp with time zone,
    results text
);


--
-- TOC entry 307 (class 1259 OID 17042)
-- Name: syncopedomain; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.syncopedomain (
    id character varying(255) NOT NULL,
    spec text
);


--
-- TOC entry 311 (class 1259 OID 17068)
-- Name: syncoperole; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.syncoperole (
    id character varying(255) NOT NULL,
    anylayout text,
    dynmembershipcond character varying(255),
    entitlements text
);


--
-- TOC entry 312 (class 1259 OID 17075)
-- Name: syncoperole_dynrealm; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.syncoperole_dynrealm (
    role_id character varying(255),
    dynamicrealm_id character varying(255)
);


--
-- TOC entry 313 (class 1259 OID 17082)
-- Name: syncoperole_realm; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.syncoperole_realm (
    role_id character varying(255),
    realm_id character varying(36)
);


--
-- TOC entry 314 (class 1259 OID 17087)
-- Name: syncopeschema; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.syncopeschema (
    id character varying(255) NOT NULL,
    labels text
);


--
-- TOC entry 315 (class 1259 OID 17094)
-- Name: syncopeuser; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.syncopeuser (
    id character varying(36) NOT NULL,
    creationcontext character varying(255),
    creationdate timestamp with time zone,
    creator character varying(255),
    lastchangecontext character varying(255),
    lastchangedate timestamp with time zone,
    lastmodifier character varying(255),
    status character varying(255),
    changepwddate timestamp with time zone,
    cipheralgorithm character varying(20),
    failedlogins integer,
    lastlogindate timestamp with time zone,
    mustchangepassword integer,
    password character varying(255),
    passwordhistory text,
    plainattrs jsonb,
    securityanswer character varying(255),
    suspended integer,
    token text,
    tokenexpiretime timestamp with time zone,
    username character varying(255),
    realm_id character varying(36),
    securityquestion_id character varying(36)
);


--
-- TOC entry 316 (class 1259 OID 17103)
-- Name: syncopeuser_anytypeclass; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.syncopeuser_anytypeclass (
    user_id character varying(36),
    anytypeclass_id character varying(255)
);


--
-- TOC entry 317 (class 1259 OID 17108)
-- Name: syncopeuser_externalresource; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.syncopeuser_externalresource (
    user_id character varying(36),
    resource_id character varying(255)
);


--
-- TOC entry 318 (class 1259 OID 17113)
-- Name: syncopeuser_syncoperole; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.syncopeuser_syncoperole (
    user_id character varying(36),
    role_id character varying(255)
);


--
-- TOC entry 319 (class 1259 OID 17118)
-- Name: ticketexpirationpolicy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ticketexpirationpolicy (
    id character varying(36) NOT NULL,
    name character varying(255),
    jsonconf text
);


--
-- TOC entry 320 (class 1259 OID 17125)
-- Name: typeextension; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.typeextension (
    id character varying(36) NOT NULL,
    group_id character varying(36),
    anytype_id character varying(255)
);


--
-- TOC entry 321 (class 1259 OID 17132)
-- Name: typeextension_anytypeclass; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.typeextension_anytypeclass (
    typeextension_id character varying(36),
    anytypeclass_id character varying(255)
);


--
-- TOC entry 372 (class 1259 OID 18696)
-- Name: udyngroupmembers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.udyngroupmembers (
    any_id character(36),
    group_id character(36)
);


--
-- TOC entry 322 (class 1259 OID 17137)
-- Name: udyngroupmembership; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.udyngroupmembership (
    id character varying(36) NOT NULL,
    fiql character varying(255),
    group_id character varying(36)
);


--
-- TOC entry 323 (class 1259 OID 17142)
-- Name: umembership; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.umembership (
    id character varying(36) NOT NULL,
    user_id character varying(36),
    group_id character varying(36)
);


--
-- TOC entry 324 (class 1259 OID 17147)
-- Name: urelationship; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.urelationship (
    id character varying(36) NOT NULL,
    user_id character varying(36),
    anyobject_id character varying(36),
    type_id character varying(255)
);


--
-- TOC entry 381 (class 1259 OID 18733)
-- Name: user_search_auxclass; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.user_search_auxclass AS
 SELECT user_id AS any_id,
    anytypeclass_id
   FROM public.syncopeuser_anytypeclass st;


--
-- TOC entry 382 (class 1259 OID 18737)
-- Name: user_search_group_res; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.user_search_group_res AS
 SELECT m.user_id AS any_id,
    st.resource_id
   FROM public.umembership m,
    public.syncopegroup r,
    public.syncopegroup_externalresource st
  WHERE (((m.group_id)::text = (r.id)::text) AND ((st.group_id)::text = (r.id)::text));


--
-- TOC entry 383 (class 1259 OID 18741)
-- Name: user_search_resource; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.user_search_resource AS
 SELECT user_id AS any_id,
    resource_id
   FROM public.syncopeuser_externalresource st;


--
-- TOC entry 384 (class 1259 OID 18745)
-- Name: user_search_role; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.user_search_role AS
 SELECT user_id AS any_id,
    role_id
   FROM public.syncopeuser_syncoperole ss;


--
-- TOC entry 385 (class 1259 OID 18749)
-- Name: user_search_umembership; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.user_search_umembership AS
 SELECT m.user_id AS any_id,
    g.id AS group_id,
    g.name AS group_name
   FROM public.umembership m,
    public.syncopegroup g
  WHERE ((m.group_id)::text = (g.id)::text);


--
-- TOC entry 386 (class 1259 OID 18753)
-- Name: user_search_urelationship; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.user_search_urelationship AS
 SELECT user_id AS any_id,
    anyobject_id AS right_any_id,
    type_id AS type
   FROM public.urelationship m;


--
-- TOC entry 325 (class 1259 OID 17154)
-- Name: waconfigentry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.waconfigentry (
    id character varying(255) NOT NULL,
    waconfigvalues text
);


--
-- TOC entry 3820 (class 2604 OID 18319)
-- Name: act_evt_log log_nr_; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_evt_log ALTER COLUMN log_nr_ SET DEFAULT nextval('public.act_evt_log_log_nr__seq'::regclass);


--
-- TOC entry 3811 (class 2604 OID 18234)
-- Name: act_hi_tsk_log id_; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_hi_tsk_log ALTER COLUMN id_ SET DEFAULT nextval('public.act_hi_tsk_log_id__seq'::regclass);


--
-- TOC entry 4771 (class 0 OID 16390)
-- Dependencies: 217
-- Data for Name: accesspolicy; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.accesspolicy VALUES ('419935c7-deb3-40b3-8a9a-683037e523a2', 'DefaultAccessPolicy', '{"_class":"org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf","order":0,"ssoEnabled":true,"caseInsensitive":true,"enabled":true,"requireAllAttributes":true}');


--
-- TOC entry 4772 (class 0 OID 16397)
-- Dependencies: 218
-- Data for Name: accesstoken; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4773 (class 0 OID 16406)
-- Dependencies: 219
-- Data for Name: accountpolicy; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.accountpolicy VALUES ('20ab5a8c-4b0c-432c-b957-f7fb9784d9f7', 'an account policy', 0, 0);
INSERT INTO public.accountpolicy VALUES ('06e2ed52-6966-44aa-a177-a0ca7434201f', 'sample account policy', 3, 0);


--
-- TOC entry 4774 (class 0 OID 16411)
-- Dependencies: 220
-- Data for Name: accountpolicyrule; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.accountpolicyrule VALUES ('20ab5a8c-4b0c-432c-b957-f7fb9784d9f7', 'DefaultAccountRuleConf1');
INSERT INTO public.accountpolicyrule VALUES ('06e2ed52-6966-44aa-a177-a0ca7434201f', 'DefaultAccountRuleConf2');


--
-- TOC entry 4906 (class 0 OID 18316)
-- Dependencies: 352
-- Data for Name: act_evt_log; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4881 (class 0 OID 18006)
-- Dependencies: 327
-- Data for Name: act_ge_bytearray; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.act_ge_bytearray VALUES ('2f285177-0816-11f1-8e1e-b6c4454722a1', 1, 'userWorkflow.bpmn20.xml', '2f285176-0816-11f1-8e1e-b6c4454722a1', '\x3c3f786d6c2076657273696f6e3d22312e302220656e636f64696e673d225554462d38223f3e0a3c212d2d0a4c6963656e73656420746f207468652041706163686520536f66747761726520466f756e646174696f6e20284153462920756e646572206f6e650a6f72206d6f726520636f6e7472696275746f72206c6963656e73652061677265656d656e74732e202053656520746865204e4f544943452066696c650a64697374726962757465642077697468207468697320776f726b20666f72206164646974696f6e616c20696e666f726d6174696f6e0a726567617264696e6720636f70797269676874206f776e6572736869702e202054686520415346206c6963656e73657320746869732066696c650a746f20796f7520756e6465722074686520417061636865204c6963656e73652c2056657273696f6e20322e3020287468650a224c6963656e736522293b20796f75206d6179206e6f742075736520746869732066696c652065786365707420696e20636f6d706c69616e63650a7769746820746865204c6963656e73652e2020596f75206d6179206f627461696e206120636f7079206f6620746865204c6963656e73652061740a0a2020687474703a2f2f7777772e6170616368652e6f72672f6c6963656e7365732f4c4943454e53452d322e300a0a556e6c657373207265717569726564206279206170706c696361626c65206c6177206f722061677265656420746f20696e2077726974696e672c0a736f66747761726520646973747269627574656420756e64657220746865204c6963656e7365206973206469737472696275746564206f6e20616e0a224153204953222042415349532c20574954484f55542057415252414e54494553204f5220434f4e444954494f4e53204f4620414e590a4b494e442c206569746865722065787072657373206f7220696d706c6965642e202053656520746865204c6963656e736520666f72207468650a7370656369666963206c616e677561676520676f7665726e696e67207065726d697373696f6e7320616e64206c696d69746174696f6e730a756e64657220746865204c6963656e73652e0a2d2d3e0a3c646566696e6974696f6e7320786d6c6e733d22687474703a2f2f7777772e6f6d672e6f72672f737065632f42504d4e2f32303130303532342f4d4f44454c22200a20202020202020202020202020786d6c6e733a7873693d22687474703a2f2f7777772e77332e6f72672f323030312f584d4c536368656d612d696e7374616e636522200a20202020202020202020202020786d6c6e733a666c6f7761626c653d22687474703a2f2f666c6f7761626c652e6f72672f62706d6e220a20202020202020202020202020786d6c6e733a62706d6e64693d22687474703a2f2f7777772e6f6d672e6f72672f737065632f42504d4e2f32303130303532342f444922200a20202020202020202020202020786d6c6e733a6f6d6764633d22687474703a2f2f7777772e6f6d672e6f72672f737065632f44442f32303130303532342f444322200a20202020202020202020202020786d6c6e733a6f6d6764693d22687474703a2f2f7777772e6f6d672e6f72672f737065632f44442f32303130303532342f444922200a20202020202020202020202020747970654c616e67756167653d22687474703a2f2f7777772e77332e6f72672f323030312f584d4c536368656d6122200a2020202020202020202020202065787072657373696f6e4c616e67756167653d22687474703a2f2f7777772e77332e6f72672f313939392f585061746822200a202020202020202020202020207461726765744e616d6573706163653d22687474703a2f2f7777772e666c6f7761626c652e6f72672f70726f63657373646566223e0a200a20203c70726f636573732069643d2275736572576f726b666c6f7722206e616d653d225573657220576f726b666c6f772220697345786563757461626c653d2274727565223e0a202020203c73746172744576656e742069643d227468655374617274222f3e0a202020203c73657175656e6365466c6f772069643d22666c6f77312220736f757263655265663d22746865537461727422207461726765745265663d22637265617465222f3e0a202020203c736572766963655461736b2069643d2263726561746522206e616d653d224372656174652220666c6f7761626c653a64656c656761746545787072657373696f6e3d22247b6372656174657d222f3e0a202020203c73657175656e6365466c6f772069643d22666c6f77322220736f757263655265663d2263726561746522207461726765745265663d226372656174654757222f3e0a202020203c6578636c7573697665476174657761792069643d226372656174654757222064656661756c743d22637265617465324163746976617465222f3e0a202020203c757365725461736b2069643d22637265617465417070726f76616c22206e616d653d2243726561746520617070726f76616c2220666c6f7761626c653a63616e64696461746547726f7570733d226d616e6167696e674469726563746f722220666c6f7761626c653a666f726d4b65793d22637265617465417070726f76616c223e0a2020202020203c657874656e73696f6e456c656d656e74733e0a20202020202020203c666c6f7761626c653a666f726d50726f70657274792069643d22757365726e616d6522206e616d653d22557365726e616d652220747970653d22737472696e67222065787072657373696f6e3d22247b75736572544f2e757365726e616d657d22207772697461626c653d2266616c7365222f3e0a20202020202020203c666c6f7761626c653a666f726d50726f70657274792069643d22617070726f766543726561746522206e616d653d22417070726f76653f2220747970653d22626f6f6c65616e22207661726961626c653d22617070726f7665437265617465222072657175697265643d2274727565222f3e0a20202020202020203c666c6f7761626c653a666f726d50726f70657274792069643d2272656a656374526561736f6e22206e616d653d22526561736f6e20666f722072656a656374696e672220747970653d22737472696e6722207661726961626c653d2272656a656374526561736f6e222f3e0a2020202020203c2f657874656e73696f6e456c656d656e74733e0a202020203c2f757365725461736b3e0a202020203c7363726970745461736b2069643d22437265617465417070726f76616c4576616c756174696f6e22206e616d653d2243726561746520617070726f76616c206576616c756174696f6e2220736372697074466f726d61743d2267726f6f76792220666c6f7761626c653a6175746f53746f72655661726961626c65733d2266616c7365223e0a2020202020203c7363726970743e3c215b43444154415b0a747279207b0a202069662028617070726f7665437265617465297b0a20202020657865637574696f6e2e7365745661726961626c6528227461736b222c2027617070726f766527293b0a20207d20656c7365207b0a20202020657865637574696f6e2e7365745661726961626c6528227461736b222c202772656a65637427293b0a20207d0a7d20636174636828457863657074696f6e20616529207b0a2020747279207b0a20202020696620287461736b20213d202764656c6574652729207b0a202020202020657865637574696f6e2e7365745661726961626c6528227461736b222c206e756c6c293b0a202020207d0a20207d20636174636828457863657074696f6e20746529207b0a20202020657865637574696f6e2e7365745661726961626c6528227461736b222c206e756c6c293b0a20207d0a7d2066696e616c6c79207b0a2020657865637574696f6e2e72656d6f76655661726961626c652822617070726f766543726561746522293b0a2020657865637574696f6e2e72656d6f76655661726961626c65282272656a656374526561736f6e22293b0a7d5d5d3e3c2f7363726970743e0a202020203c2f7363726970745461736b3e0a202020203c6578636c7573697665476174657761792069643d22637265617465417070726f76616c4757222064656661756c743d227369642d37364238324236382d303939442d343732392d423843462d443032383338364645393030222f3e0a202020203c6578636c7573697665476174657761792069643d22656e61626c654757222f3e0a202020203c73657175656e6365466c6f772069643d22656e61626c6547573247656e6572617465546f6b656e2220736f757263655265663d22656e61626c65475722207461726765745265663d2267656e6572617465546f6b656e223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b666c6f7761626c655574696c732e697355736572496e67726f75702875736572544f2c202767726f7570466f72576f726b666c6f774f7074496e27297d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d22656e61626c6547573241637469766174652220736f757263655265663d22656e61626c65475722207461726765745265663d226163746976617465223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b656e61626c6564203d3d206e756c6c7d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d22656e61626c654757324163746976652220736f757263655265663d22656e61626c65475722207461726765745265663d22616374697665223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b656e61626c65647d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d22656e61626c6547573253757370656e6465642220736f757263655265663d22656e61626c65475722207461726765745265663d2273757370656e64223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b21656e61626c65647d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c736572766963655461736b2069643d22616374697661746522206e616d653d2241637469766174652220666c6f7761626c653a64656c656761746545787072657373696f6e3d22247b6175746f41637469766174657d222f3e0a202020203c73657175656e6365466c6f772069643d22666c6f77342220736f757263655265663d22616374697661746522207461726765745265663d22616374697665222f3e0a202020203c736572766963655461736b2069643d2267656e6572617465546f6b656e22206e616d653d2247656e657261746520746f6b656e2220666c6f7761626c653a64656c656761746545787072657373696f6e3d22247b67656e6572617465546f6b656e7d222f3e0a202020203c73657175656e6365466c6f772069643d22666c6f77352220736f757263655265663d2267656e6572617465546f6b656e22207461726765745265663d2263726561746564222f3e0a202020203c757365725461736b2069643d226372656174656422206e616d653d2243726561746564222f3e0a202020203c73657175656e6365466c6f772069643d22666c6f77362220736f757263655265663d226372656174656422207461726765745265663d226f7074696e4757222f3e0a202020203c6578636c7573697665476174657761792069643d226f7074696e4757222f3e0a202020203c73657175656e6365466c6f772069643d22637265617465643241637469766174652220736f757263655265663d226f7074696e475722207461726765745265663d2272656d6f7665546f6b656e223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b757365722e636865636b546f6b656e28746f6b656e297d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d226372656174656432437265617465642220736f757263655265663d226f7074696e475722207461726765745265663d2263726561746564223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b21757365722e636865636b546f6b656e28746f6b656e297d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c7363726970745461736b2069643d2272656d6f7665546f6b656e22206e616d653d2252656d6f766520546f6b656e20616e642041637469766174652220736372697074466f726d61743d2267726f6f76792220666c6f7761626c653a6175746f53746f72655661726961626c65733d2266616c7365223e0a2020202020203c7363726970743e3c215b43444154415b0a2020202020202020757365722e72656d6f7665546f6b656e28290a2020202020205d5d3e3c2f7363726970743e0a202020203c2f7363726970745461736b3e0a202020203c73657175656e6365466c6f772069643d22666c6f77372220736f757263655265663d2272656d6f7665546f6b656e22207461726765745265663d22616374697665222f3e0a202020203c757365725461736b2069643d2261637469766522206e616d653d22416374697665222f3e0a202020203c73657175656e6365466c6f772069643d22666c6f77382220736f757263655265663d2261637469766522207461726765745265663d226163746976654777222f3e0a202020203c6578636c7573697665476174657761792069643d226163746976654777222f3e0a202020203c73657175656e6365466c6f772069643d2261637469766532557064617465417070726f76616c2220736f757263655265663d22616374697665477722207461726765745265663d22757064617465417070726f76616c223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b77664578656375746f72203d3d20757365722e676574557365726e616d65282920616e64207461736b203d3d202775706461746527200a2020202020202020616e642028217573657255522e6765744d656d626572736869707328292e6973456d7074792829297d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d226163746976653244656c657465417070726f76616c2220736f757263655265663d22616374697665477722207461726765745265663d2264656c657465417070726f76616c223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b77664578656375746f72203d3d20757365722e676574557365726e616d65282920616e64207461736b203d3d202764656c657465277d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d22616374697665325570646174652220736f757263655265663d22616374697665477722207461726765745265663d22757064617465223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b7461736b203d3d2027757064617465277d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d226163746976653253757370656e642220736f757263655265663d22616374697665477722207461726765745265663d2273757370656e64223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b7461736b203d3d202773757370656e64277d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d226163746976653244656c6574652220736f757263655265663d22616374697665477722207461726765745265663d2264656c657465223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b7461736b203d3d202764656c657465277d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d22616374697665325265717565737450617373776f726452657365742220736f757263655265663d22616374697665477722207461726765745265663d2267656e6572617465546f6b656e3450617373776f72645265736574223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b7461736b203d3d20277265717565737450617373776f72645265736574277d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d2261637469766532436f6e6669726d50617373776f726452657365742220736f757263655265663d22616374697665477722207461726765745265663d22636865636b546f6b656e34436f6e6669726d50617373776f72645265736574223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b7461736b203d3d2027636f6e6669726d50617373776f72645265736574277d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c757365725461736b2069643d22757064617465417070726f76616c22206e616d653d2255706461746520617070726f76616c2220666c6f7761626c653a63616e64696461746547726f7570733d226d616e6167696e674469726563746f722220666c6f7761626c653a666f726d4b65793d22757064617465417070726f76616c223e0a2020202020203c657874656e73696f6e456c656d656e74733e0a20202020202020203c666c6f7761626c653a666f726d50726f70657274792069643d22757365726e616d6522206e616d653d22557365726e616d652220747970653d22737472696e67222065787072657373696f6e3d22247b75736572544f2e757365726e616d657d22207772697461626c653d2266616c7365222f3e0a20202020202020203c666c6f7761626c653a666f726d50726f70657274792069643d22617070726f766555706461746522206e616d653d22417070726f76653f2220747970653d22626f6f6c65616e22207661726961626c653d22617070726f7665557064617465222072657175697265643d2274727565222f3e0a20202020202020203c666c6f7761626c653a666f726d50726f70657274792069643d2272656a656374526561736f6e22206e616d653d22526561736f6e20666f722072656a656374696e672220747970653d22737472696e6722207661726961626c653d2272656a656374526561736f6e222f3e0a2020202020203c2f657874656e73696f6e456c656d656e74733e0a202020203c2f757365725461736b3e0a202020203c7363726970745461736b2069643d22557064617465417070726f76616c4576616c756174696f6e22206e616d653d2255706461746520617070726f76616c206576616c756174696f6e2220736372697074466f726d61743d2267726f6f76792220666c6f7761626c653a6175746f53746f72655661726961626c65733d2266616c7365223e0a2020202020203c7363726970743e3c215b43444154415b0a747279207b0a202069662028617070726f7665557064617465297b0a20202020657865637574696f6e2e7365745661726961626c6528227461736b222c2027617070726f766527293b0a20207d20656c7365207b0a20202020657865637574696f6e2e7365745661726961626c6528227461736b222c202772656a65637427293b0a20207d0a7d20636174636828457863657074696f6e20616529207b0a2020747279207b0a20202020696620287461736b20213d202764656c6574652729207b0a202020202020657865637574696f6e2e7365745661726961626c6528227461736b222c206e756c6c293b0a202020207d0a20207d20636174636828457863657074696f6e20746529207b0a20202020657865637574696f6e2e7365745661726961626c6528227461736b222c206e756c6c293b0a20207d0a7d2066696e616c6c79207b0a2020657865637574696f6e2e72656d6f76655661726961626c652822617070726f766555706461746522293b0a2020657865637574696f6e2e72656d6f76655661726961626c65282272656a656374526561736f6e22293b0a7d5d5d3e3c2f7363726970743e0a202020203c2f7363726970745461736b3e0a202020203c6578636c7573697665476174657761792069643d22757064617465417070726f76616c4757222064656661756c743d227369642d45353842424332442d383833312d344346322d413739382d313442323538464535363942222f3e0a202020203c73657175656e6365466c6f772069643d227369642d45353842424332442d383833312d344346322d413739382d3134423235384645353639422220736f757263655265663d22757064617465417070726f76616c475722207461726765745265663d22757064617465417070726f76616c222f3e0a202020203c73657175656e6365466c6f772069643d22757064617465417070726f76616c4757325570646174652220736f757263655265663d22757064617465417070726f76616c475722207461726765745265663d22757064617465223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b7461736b203d3d2027617070726f7665277d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d22757064617465417070726f76616c47573252656a6563742220736f757263655265663d22757064617465417070726f76616c475722207461726765745265663d2272656a656374557064617465223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b7461736b203d3d202772656a656374277d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d227369642d42354646454243412d314642462d343537462d424335352d3339464433383731383842322220736f757263655265663d22757064617465417070726f76616c475722207461726765745265663d2264656c657465223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b7461736b203d3d202764656c657465277d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c7363726970745461736b2069643d2272656a65637455706461746522206e616d653d2252656a656374207570646174652220736372697074466f726d61743d2267726f6f76792220666c6f7761626c653a6175746f53746f72655661726961626c65733d2266616c7365223e0a2020202020203c7363726970743e3c215b43444154415b0a2020202020202020657865637574696f6e2e7365745661726961626c65282270726f7042795265736f75726365222c206e756c6c293b0a2020202020205d5d3e3c2f7363726970743e0a202020203c2f7363726970745461736b3e0a202020203c73657175656e6365466c6f772069643d22666c6f77387465722220736f757263655265663d2272656a65637455706461746522207461726765745265663d22616374697665222f3e0a202020203c736572766963655461736b2069643d2275706461746522206e616d653d225570646174652220666c6f7761626c653a64656c656761746545787072657373696f6e3d22247b7570646174657d222f3e0a202020203c73657175656e6365466c6f772069643d22666c6f77392220736f757263655265663d2275706461746522207461726765745265663d22616374697665222f3e0a202020203c736572766963655461736b2069643d2273757370656e6422206e616d653d2253757370656e642220666c6f7761626c653a64656c656761746545787072657373696f6e3d22247b73757370656e647d222f3e0a202020203c73657175656e6365466c6f772069643d22666c6f7731302220736f757263655265663d2273757370656e6422207461726765745265663d2273757370656e646564222f3e0a202020203c757365725461736b2069643d2273757370656e64656422206e616d653d2253757370656e646564222f3e0a202020203c73657175656e6365466c6f772069643d22666c6f7731312220736f757263655265663d2273757370656e64656422207461726765745265663d2273757370656e6465644777222f3e0a202020203c6578636c7573697665476174657761792069643d2273757370656e6465644777222064656661756c743d227369642d32364143444543342d384245322d344537302d424236332d333642363337354237454543222f3e0a202020203c73657175656e6365466c6f772069643d2273757370656e64656432526561637469766174652220736f757263655265663d2273757370656e646564477722207461726765745265663d2272656163746976617465223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b7461736b203d3d202772656163746976617465277d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d2273757370656e6465643244656c6574652220736f757263655265663d2273757370656e646564477722207461726765745265663d2264656c657465223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b7461736b203d3d202764656c657465277d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c736572766963655461736b2069643d227265616374697661746522206e616d653d22526561637469766174652220666c6f7761626c653a64656c656761746545787072657373696f6e3d22247b726561637469766174657d222f3e0a202020203c73657175656e6365466c6f772069643d22666c6f7731322220736f757263655265663d227265616374697661746522207461726765745265663d22616374697665222f3e0a202020203c7363726970745461736b2069643d2272656a65637422206e616d653d2252656a6563742220736372697074466f726d61743d2267726f6f76792220666c6f7761626c653a6175746f53746f72655661726961626c65733d2266616c7365223e0a2020202020203c7363726970743e3c215b43444154415b0a2020202020202020657865637574696f6e2e72656d6f76655661726961626c65282275736572544f22293b0a2020202020202020657865637574696f6e2e72656d6f76655661726961626c652822656e6372797074656450776422293b0a2020202020202020657865637574696f6e2e72656d6f76655661726961626c65282270726f7042795265736f7572636522293b0a2020202020205d5d3e3c2f7363726970743e0a202020203c2f7363726970745461736b3e0a202020203c757365725461736b2069643d2272656a656374656422206e616d653d2252656a6563746564222f3e0a202020203c73657175656e6365466c6f772069643d22666c6f7731342220736f757263655265663d2272656a656374656422207461726765745265663d2272656a65637465644777222f3e0a202020203c6578636c7573697665476174657761792069643d2272656a65637465644777222f3e0a202020203c73657175656e6365466c6f772069643d2272656a65637465643244656c6574652220736f757263655265663d2272656a6563746564477722207461726765745265663d2264656c657465223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b7461736b203d3d202764656c657465277d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d2272656a65637465643252656a65637465642220736f757263655265663d2272656a6563746564477722207461726765745265663d2272656a6563746564223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b656d707479207461736b7d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c757365725461736b2069643d2264656c657465417070726f76616c22206e616d653d2244656c65746520617070726f76616c2220666c6f7761626c653a63616e64696461746547726f7570733d226d616e6167696e674469726563746f722220666c6f7761626c653a666f726d4b65793d2264656c657465417070726f76616c223e0a2020202020203c657874656e73696f6e456c656d656e74733e0a20202020202020203c666c6f7761626c653a666f726d50726f70657274792069643d22757365726e616d6522206e616d653d22557365726e616d652220747970653d22737472696e67222065787072657373696f6e3d22247b75736572544f2e757365726e616d657d22207772697461626c653d2266616c7365222f3e0a20202020202020203c666c6f7761626c653a666f726d50726f70657274792069643d22617070726f766544656c65746522206e616d653d22417070726f76653f2220747970653d22626f6f6c65616e22207661726961626c653d22617070726f766544656c657465222072657175697265643d2274727565222f3e0a20202020202020203c666c6f7761626c653a666f726d50726f70657274792069643d2272656a656374526561736f6e22206e616d653d22526561736f6e20666f722072656a656374696e672220747970653d22737472696e6722207661726961626c653d2272656a656374526561736f6e222f3e0a2020202020203c2f657874656e73696f6e456c656d656e74733e0a202020203c2f757365725461736b3e0a202020203c73657175656e6365466c6f772069643d22666c6f7731346269732220736f757263655265663d2264656c657465417070726f76616c22207461726765745265663d2264656c657465417070726f76616c4757222f3e0a202020203c6578636c7573697665476174657761792069643d2264656c657465417070726f76616c4757222f3e0a202020203c73657175656e6365466c6f772069643d2264656c657465417070726f76616c47573244656c6574652220736f757263655265663d2264656c657465417070726f76616c475722207461726765745265663d2264656c657465223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b617070726f76657d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d2264656c657465417070726f76616c47573252656a6563742220736f757263655265663d2264656c657465417070726f76616c475722207461726765745265663d2272656a65637444656c657465223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b21617070726f76657d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c7363726970745461736b2069643d2272656a65637444656c65746522206e616d653d2252656a6563742064656c6574652220736372697074466f726d61743d2267726f6f76792220666c6f7761626c653a6175746f53746f72655661726961626c65733d2266616c7365223e0a2020202020203c7363726970743e3c215b43444154415b0a2020202020202020657865637574696f6e2e7365745661726961626c65282270726f7042795265736f75726365222c206e756c6c293b0a2020202020205d5d3e3c2f7363726970743e0a202020203c2f7363726970745461736b3e0a202020203c73657175656e6365466c6f772069643d22666c6f7731347465722220736f757263655265663d2272656a65637444656c65746522207461726765745265663d22616374697665222f3e0a202020203c736572766963655461736b2069643d2267656e6572617465546f6b656e3450617373776f7264526573657422206e616d653d2247656e657261746520746f6b656e2220666c6f7761626c653a64656c656761746545787072657373696f6e3d22247b67656e6572617465546f6b656e7d222f3e0a202020203c73657175656e6365466c6f772069643d22666c6f7731352220736f757263655265663d2267656e6572617465546f6b656e3450617373776f7264526573657422207461726765745265663d226e6f74696679345265717565737450617373776f72645265736574222f3e0a202020203c736572766963655461736b2069643d226e6f74696679345265717565737450617373776f7264526573657422206e616d653d224e6f74696669636174696f6e2220666c6f7761626c653a64656c656761746545787072657373696f6e3d22247b6e6f746966797d222f3e0a202020203c73657175656e6365466c6f772069643d22666c6f7731362220736f757263655265663d226e6f74696679345265717565737450617373776f7264526573657422207461726765745265663d22616374697665222f3e0a202020203c736572766963655461736b2069643d22636865636b546f6b656e34436f6e6669726d50617373776f7264526573657422206e616d653d22436865636b20746f6b656e2c2072656d6f766520616e64207570646174652070617373776f72642220666c6f7761626c653a64656c656761746545787072657373696f6e3d22247b70617373776f726452657365747d222f3e0a202020203c73657175656e6365466c6f772069643d22666c6f7731372220736f757263655265663d22636865636b546f6b656e34436f6e6669726d50617373776f7264526573657422207461726765745265663d226e6f7469667934436f6e6669726d50617373776f72645265736574222f3e0a202020203c736572766963655461736b2069643d226e6f7469667934436f6e6669726d50617373776f7264526573657422206e616d653d224e6f74696669636174696f6e2220666c6f7761626c653a64656c656761746545787072657373696f6e3d22247b6e6f746966797d222f3e0a202020203c73657175656e6365466c6f772069643d22666c6f7731382220736f757263655265663d226e6f7469667934436f6e6669726d50617373776f7264526573657422207461726765745265663d22616374697665222f3e0a202020203c736572766963655461736b2069643d2264656c65746522206e616d653d2244656c6574652220666c6f7761626c653a64656c656761746545787072657373696f6e3d22247b64656c6574657d222f3e0a202020203c73657175656e6365466c6f772069643d22666c6f7739392220736f757263655265663d2264656c65746522207461726765745265663d22746865456e64222f3e0a202020203c656e644576656e742069643d22746865456e64222f3e0a202020203c736572766963655461736b2069643d227570646174655768696c6550656e64696e67437265617465417070726f76616c22206e616d653d225570646174652220666c6f7761626c653a64656c656761746545787072657373696f6e3d22247b7570646174657d222f3e0a202020203c73657175656e6365466c6f772069643d227369642d37364238324236382d303939442d343732392d423843462d4430323833383646453930302220736f757263655265663d22637265617465417070726f76616c475722207461726765745265663d22637265617465417070726f76616c222f3e0a202020203c73657175656e6365466c6f772069643d227369642d42324545433531312d323932342d344139352d423042382d4533354441323638444435382220736f757263655265663d22637265617465417070726f76616c475722207461726765745265663d2272656a65637465644777223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b7461736b203d3d202764656c657465277d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c6578636c7573697665476174657761792069643d227369642d38434641333135322d313941412d343837382d414432432d393642363346364539384337222064656661756c743d22666c6f7733222f3e0a202020203c73657175656e6365466c6f772069643d22666c6f7731332220736f757263655265663d2272656a65637422207461726765745265663d2272656a6563746564222f3e0a202020203c73657175656e6365466c6f772069643d22637265617465417070726f76616c475732456e61626c6547572220736f757263655265663d22637265617465417070726f76616c475722207461726765745265663d22656e61626c654757223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b7461736b203d3d2027617070726f7665277d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d22637265617465417070726f76616c3252656a6563742220736f757263655265663d22637265617465417070726f76616c475722207461726765745265663d2272656a656374223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b7461736b203d3d202772656a656374277d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d226372656174653241637469766174652220736f757263655265663d22637265617465475722207461726765745265663d22656e61626c654757222f3e0a202020203c73657175656e6365466c6f772069643d22666c6f77332220736f757263655265663d227369642d38434641333135322d313941412d343837382d414432432d39364236334636453938433722207461726765745265663d22437265617465417070726f76616c4576616c756174696f6e222f3e0a202020203c73657175656e6365466c6f772069643d227369642d35364537303431412d373438412d344337312d414246332d3744434130424543383939312220736f757263655265663d22437265617465417070726f76616c4576616c756174696f6e22207461726765745265663d22637265617465417070726f76616c4757222f3e0a202020203c73657175656e6365466c6f772069643d226372656174654173416e6f6e796d6f757332417070726f76616c2220736f757263655265663d22637265617465475722207461726765745265663d22637265617465417070726f76616c223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b77664578656375746f72203d3d2027616e6f6e796d6f757327207c7c20666c6f7761626c655574696c732e697355736572496e67726f75702875736572544f2c202767726f7570466f72576f726b666c6f77417070726f76616c27297d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c736572766963655461736b2069643d227570646174655768696c6550656e64696e67557064617465417070726f76616c22206e616d653d225570646174652220666c6f7761626c653a64656c656761746545787072657373696f6e3d22247b7570646174657d222f3e0a202020203c6578636c7573697665476174657761792069643d227369642d34433943393131372d323644422d343332362d413132422d454544413432454144463037222064656661756c743d227369642d31324532394239342d433336392d343543312d424345462d433136354146444135323541222f3e0a202020203c73657175656e6365466c6f772069643d227369642d39333044414446312d433336312d343344442d413234302d3538324632314445423942362220736f757263655265663d22637265617465417070726f76616c22207461726765745265663d227369642d38434641333135322d313941412d343837382d414432432d393642363346364539384337222f3e0a202020203c73657175656e6365466c6f772069643d227369642d41324244463830332d363838432d344134442d394433332d3644383539433032393234352220736f757263655265663d22757064617465417070726f76616c22207461726765745265663d227369642d34433943393131372d323644422d343332362d413132422d454544413432454144463037222f3e0a202020203c73657175656e6365466c6f772069643d227369642d36364344423143342d374332462d344241452d393833342d3834434630434635324246332220736f757263655265663d227369642d34433943393131372d323644422d343332362d413132422d45454441343245414446303722207461726765745265663d227570646174655768696c6550656e64696e67557064617465417070726f76616c223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b7461736b203d3d2027757064617465277d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d227369642d43394641353941462d344633392d343839452d423432342d3543303642313541453331372220736f757263655265663d227369642d38434641333135322d313941412d343837382d414432432d39364236334636453938433722207461726765745265663d227570646174655768696c6550656e64696e67437265617465417070726f76616c223e0a2020202020203c636f6e646974696f6e45787072657373696f6e207873693a747970653d2274466f726d616c45787072657373696f6e223e3c215b43444154415b247b7461736b203d3d2027757064617465277d5d5d3e3c2f636f6e646974696f6e45787072657373696f6e3e0a202020203c2f73657175656e6365466c6f773e0a202020203c73657175656e6365466c6f772069643d227369642d46373436453738462d463539422d343834322d394438382d4339433739343734464330362220736f757263655265663d227570646174655768696c6550656e64696e67437265617465417070726f76616c22207461726765745265663d22637265617465417070726f76616c222f3e0a202020203c73657175656e6365466c6f772069643d227369642d31443044413539332d383544372d343132422d384441332d3739444330434530304531442220736f757263655265663d227570646174655768696c6550656e64696e67557064617465417070726f76616c22207461726765745265663d22757064617465417070726f76616c222f3e0a202020203c73657175656e6365466c6f772069643d227369642d31324532394239342d433336392d343543312d424345462d4331363541464441353235412220736f757263655265663d227369642d34433943393131372d323644422d343332362d413132422d45454441343245414446303722207461726765745265663d22557064617465417070726f76616c4576616c756174696f6e222f3e0a202020203c73657175656e6365466c6f772069643d227369642d35334641374632392d434536302d344145362d393231442d4146373333314344423139462220736f757263655265663d22557064617465417070726f76616c4576616c756174696f6e22207461726765745265663d22757064617465417070726f76616c4757222f3e0a202020203c73657175656e6365466c6f772069643d227369642d32364143444543342d384245322d344537302d424236332d3336423633373542374545432220736f757263655265663d2273757370656e646564477722207461726765745265663d2273757370656e646564222f3e0a20203c2f70726f636573733e0a20203c62706d6e64693a42504d4e4469616772616d2069643d2242504d4e4469616772616d5f75736572576f726b666c6f77223e0a202020203c62706d6e64693a42504d4e506c616e652062706d6e456c656d656e743d2275736572576f726b666c6f77222069643d2242504d4e506c616e655f75736572576f726b666c6f77223e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d227468655374617274222069643d2242504d4e53686170655f7468655374617274223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2233302e30222077696474683d2233302e302220783d223333332e302220793d223235322e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d22637265617465222069643d2242504d4e53686170655f637265617465223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d223330302e302220793d223332332e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d226372656174654757222069643d2242504d4e53686170655f6372656174654757223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2234302e30222077696474683d2234302e302220783d223333302e302220793d223530372e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d22637265617465417070726f76616c222069643d2242504d4e53686170655f637265617465417070726f76616c223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d223432302e302220793d223536302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d22437265617465417070726f76616c4576616c756174696f6e222069643d2242504d4e53686170655f437265617465417070726f76616c4576616c756174696f6e223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2238302e30222077696474683d223130302e302220783d223437372e352220793d223736352e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d22637265617465417070726f76616c4757222069643d2242504d4e53686170655f637265617465417070726f76616c4757223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2234302e30222077696474683d2234302e302220783d223537302e302220793d223537302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d22656e61626c654757222069643d2242504d4e53686170655f656e61626c654757223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2234302e30222077696474683d2234302e302220783d223639302e302220793d223432392e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d226163746976617465222069643d2242504d4e53686170655f6163746976617465223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d223831302e302220793d223631302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d2267656e6572617465546f6b656e222069643d2242504d4e53686170655f67656e6572617465546f6b656e223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d223831302e302220793d223231332e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d2263726561746564222069643d2242504d4e53686170655f63726561746564223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d223935322e302220793d223231332e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d226f7074696e4757222069643d2242504d4e53686170655f6f7074696e4757223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2234302e30222077696474683d2234302e302220783d22313130322e302220793d223232332e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d2272656d6f7665546f6b656e222069643d2242504d4e53686170655f72656d6f7665546f6b656e223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d22313138302e302220793d223231332e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d22616374697665222069643d2242504d4e53686170655f616374697665223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d22313133302e302220793d223531312e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d226163746976654777222069643d2242504d4e53686170655f6163746976654777223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2234302e30222077696474683d2234302e302220783d22313530302e302220793d223532302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d22757064617465417070726f76616c222069643d2242504d4e53686170655f757064617465417070726f76616c223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d22313630352e302220793d223734302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d22557064617465417070726f76616c4576616c756174696f6e222069643d2242504d4e53686170655f557064617465417070726f76616c4576616c756174696f6e223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2238302e30222077696474683d223130302e302220783d22313734302e302220793d223835352e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d22757064617465417070726f76616c4757222069643d2242504d4e53686170655f757064617465417070726f76616c4757223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2234302e30222077696474683d2234302e302220783d22313737302e302220793d223735302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d2272656a656374557064617465222069643d2242504d4e53686170655f72656a656374557064617465223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d22313839302e302220793d223834302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d22757064617465222069643d2242504d4e53686170655f757064617465223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d22313839302e302220793d223730302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d2273757370656e64222069643d2242504d4e53686170655f73757370656e64223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d22313539302e302220793d223130302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d2273757370656e646564222069643d2242504d4e53686170655f73757370656e646564223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d22313734302e302220793d223132302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d2273757370656e6465644777222069643d2242504d4e53686170655f73757370656e6465644777223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2234302e30222077696474683d2234302e302220783d22313932302e302220793d223138302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d2272656163746976617465222069643d2242504d4e53686170655f72656163746976617465223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d22323034302e302220793d223131302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d2272656a656374222069643d2242504d4e53686170655f72656a656374223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d223636302e302220793d223635392e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d2272656a6563746564222069643d2242504d4e53686170655f72656a6563746564223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d223831302e302220793d223737302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d2272656a65637465644777222069643d2242504d4e53686170655f72656a65637465644777223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2234302e30222077696474683d2234302e302220783d223939302e302220793d223738302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d2264656c657465417070726f76616c222069643d2242504d4e53686170655f64656c657465417070726f76616c223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d22313539302e302220793d223238302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d2264656c657465417070726f76616c4757222069643d2242504d4e53686170655f64656c657465417070726f76616c4757223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2234302e30222077696474683d2234302e302220783d22313737302e302220793d223330302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d2272656a65637444656c657465222069643d2242504d4e53686170655f72656a65637444656c657465223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d22313839302e302220793d223234302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d2267656e6572617465546f6b656e3450617373776f72645265736574222069643d2242504d4e53686170655f67656e6572617465546f6b656e3450617373776f72645265736574223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2235392e30222077696474683d223130302e302220783d22313634332e302220793d223435322e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d226e6f74696679345265717565737450617373776f72645265736574222069643d2242504d4e53686170655f6e6f74696679345265717565737450617373776f72645265736574223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d22313834382e302220793d223435322e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d22636865636b546f6b656e34436f6e6669726d50617373776f72645265736574222069643d2242504d4e53686170655f636865636b546f6b656e34436f6e6669726d50617373776f72645265736574223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d22313634332e302220793d223536312e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d226e6f7469667934436f6e6669726d50617373776f72645265736574222069643d2242504d4e53686170655f6e6f7469667934436f6e6669726d50617373776f72645265736574223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d22313834382e302220793d223536312e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d2264656c657465222069643d2242504d4e53686170655f64656c657465223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d22323232302e302220793d223335382e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d22746865456e64222069643d2242504d4e53686170655f746865456e64223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2232382e30222077696474683d2232382e302220783d22323335382e302220793d223337342e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d227570646174655768696c6550656e64696e67437265617465417070726f76616c222069643d2242504d4e53686170655f7570646174655768696c6550656e64696e67437265617465417070726f76616c223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d223336322e352220793d223737302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d227369642d38434641333135322d313941412d343837382d414432432d393642363346364539384337222069643d2242504d4e53686170655f7369642d38434641333135322d313941412d343837382d414432432d393642363346364539384337223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2234302e30222077696474683d2234302e302220783d223435302e302220793d223636302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d227570646174655768696c6550656e64696e67557064617465417070726f76616c222069643d2242504d4e53686170655f7570646174655768696c6550656e64696e67557064617465417070726f76616c223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2236302e30222077696474683d223130302e302220783d22313435352e302220793d223836352e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e53686170652062706d6e456c656d656e743d227369642d34433943393131372d323644422d343332362d413132422d454544413432454144463037222069643d2242504d4e53686170655f7369642d34433943393131372d323644422d343332362d413132422d454544413432454144463037223e0a20202020202020203c6f6d6764633a426f756e6473206865696768743d2234302e30222077696474683d2234302e302220783d22313633352e302220793d223837352e30222f3e0a2020202020203c2f62706d6e64693a42504d4e53686170653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22656e61626c6547573247656e6572617465546f6b656e222069643d2242504d4e456467655f656e61626c6547573247656e6572617465546f6b656e223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223732392e393433333534343330323730372220793d223434392e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223836302e302220793d223434392e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223836302e302220793d223237322e3935222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d2261637469766532557064617465222069643d2242504d4e456467655f61637469766532557064617465223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313533392e393232333638343231303236372220793d223534302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313535362e302220793d223534302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313535362e302220793d223733302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313838392e3939393939393939393730312220793d223733302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d227369642d31443044413539332d383544372d343132422d384441332d373944433043453030453144222069643d2242504d4e456467655f7369642d31443044413539332d383544372d343132422d384441332d373944433043453030453144223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313534302e39342220793d223836352e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313631392e302220793d223739392e39343939393939393939393939222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d2261637469766532436f6e6669726d50617373776f72645265736574222069643d2242504d4e456467655f61637469766532436f6e6669726d50617373776f72645265736574223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313533392e393434323337343335303038352220793d223534302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313639332e302220793d223534302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313639332e302220793d223536312e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d227369642d37364238324236382d303939442d343732392d423843462d443032383338364645393030222069643d2242504d4e456467655f7369642d37364238324236382d303939442d343732392d423843462d443032383338364645393030223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223537302e302220793d223539302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223531392e393439393939393939373339322220793d223539302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d227369642d39333044414446312d433336312d343344442d413234302d353832463231444542394236222069643d2242504d4e456467655f7369642d39333044414446312d433336312d343344442d413234302d353832463231444542394236223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223437302e302220793d223631392e39343939393939393939393939222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223437302e302220793d223636302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d2272656a65637465643252656a6563746564222069643d2242504d4e456467655f72656a65637465643252656a6563746564223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223939302e302220793d223830302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223930392e393439393939393939393133352220793d223830302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d227369642d35364537303431412d373438412d344337312d414246332d374443413042454338393931222069643d2242504d4e456467655f7369642d35364537303431412d373438412d344337312d414246332d374443413042454338393931223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223537372e343439393939393939393438382220793d223830352e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223539302e302220793d223830352e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223539302e302220793d223630392e39303436313834323731373535222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d226163746976653244656c657465222069643d2242504d4e456467655f6163746976653244656c657465223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313533392e393138393235323333363434382220793d223534302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313535322e302220793d223534302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313535322e302220793d223638302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22323137342e302220793d223638302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22323236302e313336393836333031333639372220793d223431372e3935222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f7731222069643d2242504d4e456467655f666c6f7731223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223334382e333437353832323634383332382220793d223238312e3934353834333130343634303633222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223334392e33303233323535383133393533342220793d223332332e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f7732222069643d2242504d4e456467655f666c6f7732223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223335302e302220793d223338322e3935222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223335302e302220793d223530372e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f7733222069643d2242504d4e456467655f666c6f7733223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223438392e39333235333036343739383539372220793d223638302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223532372e302220793d223638302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223532372e33342220793d223736352e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f7734222069643d2242504d4e456467655f666c6f7734223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223930392e393439393939393939383939342220793d223634302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223932322e302220793d223634302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313035392e302220793d223634302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313134332e333333333333333333333333332220793d223537302e3935222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22757064617465417070726f76616c475732557064617465222069643d2242504d4e456467655f757064617465417070726f76616c475732557064617465223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313830392e393333393337313938303637352220793d223737302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313835322e302220793d223737302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313835322e302220793d223733302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313839302e302220793d223733302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f7735222069643d2242504d4e456467655f666c6f7735223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223930392e393439393939393939393339332220793d223234332e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223935322e302220793d223234332e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f7736222069643d2242504d4e456467655f666c6f7736223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313030322e302220793d223237322e3935222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313030322e302220793d223331302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313132322e302220793d223331302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313132322e302220793d223236322e39313438333535373534383538222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f7737222069643d2242504d4e456467655f666c6f7737223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313232342e39363634343239353330322220793d223237322e3935222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313138352e3032353136373738353233352220793d223531312e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d227369642d36364344423143342d374332462d344241452d393833342d383443463043463532424633222069643d2242504d4e456467655f7369642d36364344423143342d374332462d344241452d393833342d383443463043463532424633223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313633352e302220793d223839352e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313535342e39352220793d223839352e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f7738222069643d2242504d4e456467655f666c6f7738223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313232392e393439393939393939393931342220793d223534302e38353239343131373634373037222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313530302e303538363531303236333732342220793d223534302e303538353034333938383237222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d226163746976653253757370656e64222069643d2242504d4e456467655f6163746976653253757370656e64223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313533392e393138393235323333363434382220793d223534302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313535322e302220793d223534302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313535322e302220793d223133302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313538392e393939393939393939393736382220793d223133302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f7739222069643d2242504d4e456467655f666c6f7739223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313938392e39352220793d223733302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22323033372e302220793d223733302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22323033372e302220793d223937312e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313333342e302220793d223937312e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313138302e302220793d223834342e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313138302e302220793d223537302e3935222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d2261637469766532557064617465417070726f76616c222069643d2242504d4e456467655f61637469766532557064617465417070726f76616c223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313533392e393230373437383030353039322220793d223534302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313535342e302220793d223534302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313535342e302220793d223737302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313630352e302220793d223737302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d227369642d42354646454243412d314642462d343537462d424335352d333946443338373138384232222069643d2242504d4e456467655f7369642d42354646454243412d314642462d343537462d424335352d333946443338373138384232223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313739302e302220793d223738392e39333034373934353230353438222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313739302e302220793d223832312e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22323237302e302220793d223832312e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22323237302e302220793d223431372e3935222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d2264656c657465417070726f76616c47573244656c657465222069643d2242504d4e456467655f64656c657465417070726f76616c47573244656c657465223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313830392e393333393337313938303637372220793d223332302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313835322e302220793d223332302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313835322e302220793d223338382e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22323232302e302220793d223338382e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d226372656174654173416e6f6e796d6f757332417070726f76616c222069643d2242504d4e456467655f6372656174654173416e6f6e796d6f757332417070726f76616c223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223336392e39313839323532333336343438372220793d223532372e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223338322e302220793d223532372e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223338322e302220793d223539302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223432302e302220793d223539302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22637265617465417070726f76616c3252656a656374222069643d2242504d4e456467655f637265617465417070726f76616c3252656a656374223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223630392e393138393235323333363434372220793d223539302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223632322e302220793d223539302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223632322e302220793d223638392e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223636302e302220793d223638392e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d227369642d35334641374632392d434536302d344145362d393231442d414637333331434442313946222069643d2242504d4e456467655f7369642d35334641374632392d434536302d344145362d393231442d414637333331434442313946223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313739302e302220793d223835352e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313739302e302220793d223738392e39303739343633353730383536222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d2272656a65637465643244656c657465222069643d2242504d4e456467655f72656a65637465643244656c657465223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313032392e393333393337313938303637352220793d223830302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313037322e302220793d223830302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313332332e302220793d22313030302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22323237302e302220793d22313030302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22323237302e302220793d223431372e3935222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d227369642d46373436453738462d463539422d343834322d394438382d433943373934373446433036222069643d2242504d4e456467655f7369642d46373436453738462d463539422d343834322d394438382d433943373934373446433036223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223430302e38333333333333333333333333372220793d223737302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223336372e302220793d223638332e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223433362e373734313933353438333837312220793d223631392e39343939393939393939393939222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22656e61626c65475732416374697665222069643d2242504d4e456467655f656e61626c65475732416374697665223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223732392e393433333938343131353533342220793d223434392e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223836312e302220793d223434392e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223836312e302220793d223534312e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313133302e302220793d223534312e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f773134746572222069643d2242504d4e456467655f666c6f773134746572223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313838392e393939393939393939393939382220793d223236302e37343531393233303736393233222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313532342e302220793d223139332e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313230392e363035373437313236343336372220793d223531312e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d227369642d32364143444543342d384245322d344537302d424236332d333642363337354237454543222069643d2242504d4e456467655f7369642d32364143444543342d384245322d344537302d424236332d333642363337354237454543223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313934302e302220793d223138302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313934302e302220793d223133352e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313833392e39352220793d223133352e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d2273757370656e6465643244656c657465222069643d2242504d4e456467655f73757370656e6465643244656c657465223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313935392e393436393738313838343237372220793d223230302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22323237302e302220793d223230302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22323237302e302220793d223335382e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d2264656c657465417070726f76616c47573252656a656374222069643d2242504d4e456467655f64656c657465417070726f76616c47573252656a656374223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313830392e393333393337313938303637372220793d223332302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313835322e302220793d223332302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313835322e302220793d223237302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313838392e393939393939393939393736382220793d223237302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22637265617465417070726f76616c475732456e61626c654757222069643d2242504d4e456467655f637265617465417070726f76616c475732456e61626c654757223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223630392e393138393235323333363434372220793d223539302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223632322e302220793d223539302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223632322e302220793d223434392e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223639302e302220793d223434392e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22637265617465643243726561746564222069643d2242504d4e456467655f637265617465643243726561746564223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313130322e302220793d223234332e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313035312e39352220793d223234332e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d2273757370656e6465643252656163746976617465222069643d2242504d4e456467655f73757370656e6465643252656163746976617465223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313935392e393333393337313938303431332220793d223230302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22323030322e302220793d223230302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22323030322e302220793d223134302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22323033392e3939393939393939393937372220793d223134302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d226163746976653244656c657465417070726f76616c222069643d2242504d4e456467655f6163746976653244656c657465417070726f76616c223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313533392e393230373437383030353039322220793d223534302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313535342e302220793d223534302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313535342e302220793d223331302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313539302e302220793d223331302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f773137222069643d2242504d4e456467655f666c6f773137223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313734322e39352220793d223539312e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313834382e302220793d223539312e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f773138222069643d2242504d4e456467655f666c6f773138223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313834372e393939393939393939393939382220793d223631392e33373136303030303030303031222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313737332e302220793d223636322e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313232392e393439393939393939393939382220793d223535312e31393231353835313630323032222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d227369642d41324244463830332d363838432d344134442d394433332d364438353943303239323435222069643d2242504d4e456467655f7369642d41324244463830332d363838432d344134442d394433332d364438353943303239323435223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313635352e302220793d223739392e39343939393939393939393939222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313635352e302220793d223837352e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f773133222069643d2242504d4e456467655f666c6f773133223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223735392e393439393939393939383939342220793d223638392e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223737322e302220793d223638392e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223737322e302220793d223830302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223831302e302220793d223830302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f773134222069643d2242504d4e456467655f666c6f773134223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223836302e302220793d223737302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223836302e302220793d223733322e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313031302e302220793d223733322e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313031302e302220793d223738302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f773135222069643d2242504d4e456467655f666c6f773135223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313734322e39352220793d223438312e3632313832393236383239323734222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313834372e393939393939393939393938392220793d223438312e38373830343837383034383738222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d227369642d42324545433531312d323932342d344139352d423042382d453335444132363844443538222069643d2242504d4e456467655f7369642d42324545433531312d323932342d344139352d423042382d453335444132363844443538223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223539302e302220793d223630392e39343632393331393935353339222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223539302e302220793d223835392e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313031302e302220793d223835392e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313031302e302220793d223831392e39313638353035393432323734222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22757064617465417070726f76616c47573252656a656374222069643d2242504d4e456467655f757064617465417070726f76616c47573252656a656374223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313830392e393333393337313938303637352220793d223737302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313835322e302220793d223737302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313835322e302220793d223837302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313839302e302220793d223837302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f773136222069643d2242504d4e456467655f666c6f773136223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313834382e302220793d223435382e3934313734373537323831353533222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313639322e302220793d223338372e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313232392e393439393939393939393939382220793d223532352e39363039333735222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d227369642d43394641353941462d344633392d343839452d423432342d354330364231354145333137222069643d2242504d4e456467655f7369642d43394641353941462d344633392d343839452d423432342d354330364231354145333137223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223435302e302220793d223638302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223431322e302220793d223638302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223431322e3337352220793d223737302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f773134626973222069643d2242504d4e456467655f666c6f773134626973223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313638392e3934393939393939393938342220793d223331302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313730322e302220793d223331302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313730322e302220793d223332302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313737302e302220793d223332302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d2263726561746564324163746976617465222069643d2242504d4e456467655f63726561746564324163746976617465223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313134312e393430373732343332383839322220793d223234332e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313138302e302220793d223234332e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d227369642d31324532394239342d433336392d343543312d424345462d433136354146444135323541222069643d2242504d4e456467655f7369642d31324532394239342d433336392d343543312d424345462d433136354146444135323541223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313637342e393432363136353739383730322220793d223839352e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313733392e393939393939393939373631352220793d223839352e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d227369642d45353842424332442d383833312d344346322d413739382d313442323538464535363942222069643d2242504d4e456467655f7369642d45353842424332442d383833312d344346322d413739382d313442323538464535363942223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313737302e302220793d223737302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313730342e39352220793d223737302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22656e61626c654757324163746976617465222069643d2242504d4e456467655f656e61626c654757324163746976617465223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223732392e393333393337313938303036312220793d223434392e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223737322e302220793d223434392e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223737322e302220793d223634302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223831302e302220793d223634302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22656e61626c6547573253757370656e646564222069643d2242504d4e456467655f656e61626c6547573253757370656e646564223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223732392e393333393337313938303036312220793d223434392e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223737322e302220793d223434392e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223737322e302220793d223133302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313539302e302220793d223133302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22637265617465324163746976617465222069643d2242504d4e456467655f637265617465324163746976617465223e0a20202020202020203c6f6d6764693a776179706f696e7420783d223336392e39313839323532333336343438372220793d223532372e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223338322e302220793d223532372e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223338322e302220793d223434392e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d223639302e302220793d223434392e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22616374697665325265717565737450617373776f72645265736574222069643d2242504d4e456467655f616374697665325265717565737450617373776f72645265736574223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313533392e393434323337343335303038352220793d223534302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313639332e302220793d223534302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313639332e302220793d223531302e3935222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f773130222069643d2242504d4e456467655f666c6f773130223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313638392e39352220793d223133302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313730322e302220793d223133302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313730322e302220793d223135302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313733392e393939393939393939393736382220793d223135302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f773131222069643d2242504d4e456467655f666c6f773131223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313833392e39352220793d223136352e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313837392e302220793d223136352e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313837392e302220793d223230302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313932302e302220793d223230302e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f773939222069643d2242504d4e456467655f666c6f773939223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22323331392e393439393939393939393837352220793d223338382e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22323335382e302220793d223338382e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f773132222069643d2242504d4e456467655f666c6f773132223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22323034302e302220793d223132392e3531363132393033323235383038222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22323032382e302220793d223132372e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313437382e302220793d2234302e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313139372e383134353730383538323833332220793d223531312e30222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a2020202020203c62706d6e64693a42504d4e456467652062706d6e456c656d656e743d22666c6f7738746572222069643d2242504d4e456467655f666c6f7738746572223e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313934302e302220793d223839392e39343939393939393939393939222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313934302e302220793d223934382e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313333392e302220793d223934382e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313231342e302220793d223833372e30222f3e0a20202020202020203c6f6d6764693a776179706f696e7420783d22313138332e343430323032373032373032372220793d223537302e3935222f3e0a2020202020203c2f62706d6e64693a42504d4e456467653e0a202020203c2f62706d6e64693a42504d4e506c616e653e0a20203c2f62706d6e64693a42504d4e4469616772616d3e0a3c2f646566696e6974696f6e733e', false);
INSERT INTO public.act_ge_bytearray VALUES ('2f69a038-0816-11f1-8e1e-b6c4454722a1', 1, 'userWorkflow.userWorkflow.png', '2f285176-0816-11f1-8e1e-b6c4454722a1', '\x89504e470d0a1a0a0000000d494844520000095c000003f208060000008a5c86de0000800049444154785eecdd0b9c1d657d077c82dc44a02017958b6251aa58106f54946a045a1a3511adb33373968d5baaa9a216a5204845a202054145b02a88051150b0a278030c84105044908b22d65bb82704f0821212fbbeaff3fe9f93dd34cc6e92ddcd5e66f77cbf9fcfeff39c33cf9cd99c3373e672e69f990d360000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a69a1933666c551f060000000000000000404d9ee7bb1545f1e7c82f225f8f9c5c96e55ba2fd9b2ccbfea23e3e000000000000000040c72a8ae2ab91e3f33c7f7ee44df1f8d8c817233745fe18591cb93afafe33f2aeaeaeae03b32cdba93e1d00000000000000008029ad2ccbbf2b8ae2573366ccd8b4ded7afbbbb7be7345ee4dd31eea7a39d1fed9268ff10ed0f23e7c7e3f747fbc6689f3767ce9c8debd300000000000000000098d45261545996ff5314c5ebea7d43d1dbdbbb759ee72f8fd7f7467b4ab497152b6f4bb83cf2b318f6b5684f8a7676e465dddddd5bd5a701000000000000000030299465f9bea228be591fbebeb22cdb24a6fd82c83fc6f43f10ed05d1fea858797bc2fb235745ce8cbc33faf68f76c7fa340000000000000000001a231539451ecef37cb77adf189a9665d933e36ffe7de4f0f8fb9f892c883c1879347263e4bce83ba62ccb8363dcbf9a3e7dfa46f5890000000000000000008caba2282e8a7ca43e7ca2b45aad6dcab2dc37fe4d8746fbd168bf19f965b1f2f68477462e8d9c18e989bc34cbb22dead300000000000000000018756559beba288abb67ce9cb979bdaf6966cc98b1699ee77f1dc9e2df7c5c5fa1d82d91c722f7c57b99177d67447b585757d76b0e39e49067d4a70100000000000000003022e9167d799effb82ccb7facf74d32d35aadd6b3e27dfc43e43d45519c15edb5d13e14f97de407917323474766c57bde3dcbb227d527020000000000000000b0464551fc6be4bbf5e1534996654fedeaea7a65bccf7fcef3fcb468bf15f975647959963f8d61ff1ded09d176b75aad97f4f4f43ca53e0d0000000000000000a0c3f5f4f4ec90ae005596e5f3ea7d9da0b7b777b378ef7b45bae273383eda2f457b5b6459e4dec895914f46de1e991efd4fab4f030000000000000000e81045519c5b96e547ebc33bdddcb97337ccb2ecd9799ecf881c119fd3d991eb220fc7f3df45fbfdf8dcfe2b1e1f15edcc18f7396e4f08000000000000000053589ee72f2f8ae2fe2ccbb6a8f7b166b367cfdeb62ccbfd226f8d7c2c3ec76fc7e7b828f278e42791af443e1c69455e3473e6cccdebd300000000000000000026917405a7a2287e948a82ea7d8c4c96654fcef3fc85f199e691b9912f476e2f56de9ef0eee8bb22f289b22cffa5ababeb55e9768ef5690000000000000000000d948a7e22d7d68733fa52715babd5facbc86be333ffb73ccf3f5714c5f5914722bf897c2f724ee4c8c8eba27fb7f49afa7400000000000000008009906e895714c5d2eeeeee3deb7d8caf2ccbb68f79f1b79139918f472e8fdc15793ccff31f477b7159961f8ac745abd5da3b5d45ab3e0d0000000000000000600ce579fed9a2283e591f4e73cc9c3973f3546055aef4a1c8257d05588ff715647da7af40eb6da9602bfab7ab4f030000000000000000584f799ebfb8288ac55996fd45bd8fe64bb71a4cb71c8cbcbe58790bc2cf172b6f49f8db62e52d0aaf4bb72c8c1c916e6118f3f9d96e4f0800000000000000002333ad288a1bf23cffa77a07935f4f4fcf0e6559be3af22f91d3633e5f11f3fb9ec8b2c86d912f478e8fe4d1bf576f6fef66f5690000000000000000007d8aa2e84d0557f1705abd8fa9aba7a7e7297d57366b453e12f94ae48e62e5ed097f1d7ddf8e9c5696e55bbbbaba5e397bf6ec6debd30000000000000000808e926e215814c5e256abf5927a1f9d29968927c5f2f0dcb22c6746de17f9af549097e7f9efa27d28b2307276e4bd316c46b4bb6ea0580f00000000000000804e5014c527f33cff6c7d380c26cbb2a7c732333d969977a46527f2ddc8bd916531ecd6b22cbf14ed07a3edeaeeeede73c68c199bd6a701000000000000000093522a88298a62a95bc5b1beb22cdb225d25ad2ccb432227c472f5d5687f1aedf2c8af22df8a9c1af9e7c82b62fca7d6a70100000000000000008d5614c582c8dbebc361b44c9f3e7da33ccf778fbc2196b5a323e7457e10f97d2af62bcbf2da7485b568dfd36ab50e8a3c6b03b7270400000000000000a069ca5014c58fe6ce9dbb61bd0fc6c321871cf28c580cf78f1c16cbe299d1ce8bf6fec863915b2217468e8bbc39cff3bf767b4200000000000000002644bafd5b5114f79565b96fbd0f26daac59b3b6ccf3fc65b18cf6444e8a5c1af959b1f2f684bf8cbe6fc4b2fbd168ff292dc3bdbdbd5bd7a7010000000000000000a32615ab1445716e7d383459ba3d612cbbcf8b1c9ce7f931d17e2196e31b238f461e8c5c13f94ce45fa3ffefa3dd6503b7270400000000000000607da48295a2281eeae9e9d9a1de0793552cd33b460e88bc33f2a9c8559107227f8cdc1ccbfd05799eff7bb4ff9865d91e914dead3000000000000000080018aa2f86e9ee787d787c354d4ddddbd55abd5da2796f9d96559fe47b45f8bf67f8a95b727fc79e4b2c8c991dec8df6459f617f5690000000000000000d0a1f23c7f5351143f49b766abf74127993367cec6f17d787e7c1fde183936727ee4a6b22cff10ede268e747ff7f46fbeeaeaeae03bbbbbb77ae4f030000000000000080292ccbb227174571775996afaef701ff271557a522ab546cd5577435bfaf082b1563ddd4579c958ab4de988ab652f1567d1a00000000000000004c7245517c2472517d383034e97683e9b683c5cadb0fa6db10a6db11a6db122e4fb729ecbb5d61ba6de1ec741bc3743bc3fa3400000000000000009804f23cdfad288a87233bd6fb80f59365d926913dcab2fcc7f8aefd7bb417c477ede6c81f230f44ae8a7c2af2cec801be8700000000000000000d5714c537cbb27c5f7d3830a6a6c5776f973ccfff3eda7f8d7c26724de4c1c8a3911be37bf985e83f26da83b32cfbabe9d3a76f549f080000000000000000e3a8288ad7a5db9dcd993367e37a1f30317a7b7bb78eefe5be799eff53b41f8df61bf15dfd656479e4cec8a59113233dd1f7b259b3666d599f060000000000000000a36cc68c199b1645f1ab74859d7a1fd03ce93b1bdfd7bf8eefed9b23c7452e8cdc12792c727f5996f3a23d33dac322fb1f72c821cfa84f030000000000000080112a8ae20391afd6870393ceb456abf5acc8416559be27cff3cf467b6d7cbf97467e1ff941e4bcc8d1d1f786c8ee6e4f0800000000000000300c59963db3288a87539146bd0f983ae2bbfed4f8aebf22f2cf915323df8afc2ab2bc2ccb9fa6a2cb684f881c12eb8397c4f85bd4a70100000000000000d0f1f23cffefa2288eab0f073a43ba3d617777f79e655976c5fae083d17e29da5b63bdb02c726fe4bb914fc6b077443b3dcbb2a7d7a70100000000000000d011bababa0e2c8ae2d7a9e0a2de0774bc69b17ed835cff319d1be3772766461e4e118f6bb686f28cbf2bf22ef8bcc6cb55acfcdb2ec49f58900000000000000004c0973e6ccd9b8288a9fa542897a1fc0dacc9e3d7bdb5877ec17796be463799e7f3bd6278b228f47ee887c25f291482bfa5edcd3d3f394fa3400000000000000002695a2288e8c7cab3e1c60a47a7b7b372bcb72af58b7e491e3235f8edc56acbc3de13d799e5f11fda747fe25f2ea9e9e9e1dead30000000000000000689ca228768c3c9c65d973ea7d00a36deedcb91bc6fae6d9ad56ebb5799e1f11f95cac83ae8b3c12f96de47b91cfa742d0e87b7d64b7f49afa7400000000000000002644511417966579427d38c0788b75d176b14efadbc8db221f8f7c277257e4f13ccf7f1cfd97443e14295badd6de3367cedcbc3e0d000000000000008031d3d5d5f5aa746b2f450b40936559f6e4546095e779910aae62bd75712ac04a85587d055997f71568cd49055b31fef6f5690000000000000000ac972ccb9e5414c5ed9137d7fb00268374abc174cbc1588fbd2e7264e49c62e52d097f53acbc45e1f5e996856559fe5bba8561e42fdd9e10000000000000001891b22cdf1d99571f0e3015f4f4f4ec90aee217ebb97fc9f3fc13912b8aa2b83bb2acafd8f4cb91b991e8ca5f98aea2559f0600000000000000405b2a44288ae2a13ccf9f5fef0398cad22d5463fdf7a2482bf2e1c857223f2956de9e7051ac17bf5d96e5c7226f8dec377bf6ec6debd300000000000000003a4c5996ff5514c5a9f5e1009d2add6635f29c583fceccf3fca8bef5e4f7e3f1efa27d38b2307276e4bd316c46b4bbba3d210000000000000074803ccf5f5e14c5fdb366cddab2de07c04065593e2dd69bd3236f8f7c327265e4de62e5ed096f8bfe2f457b7cb45d91bd7a7b7b37ab4f03000000000000009884d2d5588aa2b839cff3ee7a1f00c3d3d3d3f39456abf592b44e2dcbf28468ff3bda9fc67a7679e4d7916fc5b0d3a2fde7aeaeae576659f6d4fa340000000000000080062b8a624e64617d3800a327dd9e30cff3dd637d3b2b7274e4dcc80f22bf8f3c5496e5b5d19e15ed7b22ffd06ab59e152f9b569f0e000000000000003081d295558aa27830ddeeaade07c0f838e490439ed1d5d5f59a58171f96e7f919d1ce8b75f37d91c722b7442e8a1c177d59e4af67cc98b1697d1a00000000000000c038288ae233e9e47e7d3800132fcbb22d623dfdd2484fe4c4c8a5913b8b95b727fc65e49b65597e34da43a3ddb7d56a6d539f0600000000000000304af23c7f7151144b7a7b7bb7aef701d05cd3a74fdf28cbb2bf2acbf2e058971f13ed17627d7e63e4d1c88391057d05b58747fe3ec67de6066e4f0800000000000000eb655a5114df4f5745a977003079c57a7dc7c801917746ce8c5c15b93ff2c7c88fcab2bc20da0f44fb8f91176459b6497d1a00000000000000404d59966f298ae2071bb8e2094047e8eeeede2acff3974566c7faffa468bf16edcf8a95b727fc45e4b218764ab4bdd1bedcd50f01000000000000a04f96657f5114c5e2c84beb7d0074963973e66c5c96e5f3629bf0c668df1fedf9911fc6e33f44bb24daf9d17e3ada7747feaebbbb7be7fa3400000000000000604a2bcbf2f4a228ceaa0f0780d56559b6535757d781799ebf2bf29fb1edb8baaf6037dd9ef0a6c81723c746df9b22cf4fc55bf56900000000000000c0a496e7f95f1745b174f6ecd9dbd6fb006028faae94f8377db7a73d39f2f5c8cf23cb63d8ffa4e7d1fe47ea6fb55afba4db19d6a70100000000000000934251140bf23c7f477d388c4455559b2c5ab4e8921b6eb8e17fafbefaea6adebc7932ce89cffdcf0b172e5c3c7ffefc37d4e70f8cb72ccb3689ecd177c5ab7fefbb02d6cd7d57c47aa0587985ac4fa52b66457b40ba82567d1a00000000000000d018799e17e196b973e76e58ef839158b468d157162e5c582d5dbab45abe7c79f5a73ffd49c639e9734f9fff82050b1e9d376fdeebeaf3081a625a6c7f7629cbf2efa2fdd7c8a723d74496c4b03f44fbc368bf10797fe4e0c8f3dc9e1000000000000080099565d9164551dc177945bd0f462a5dd92a15fbd48b8064fcb364c99247e6cd9bf7c3fa3c82a6ebededdd3acff39747fe29724ae41bb1adfa456479e467914b2327457aa2ef65b366cddab23e0d000000000000001875e924765114e7d587c3fa48b7117465ab6624cd8779f3e6ada8cf2398acd2ed09cbb27c416cbbde1cf940e4c2c88f228f45ee8fbe79d19e19ed6191fde3f18ef56900000000000000c0886459f65745513c5496e5d3ea7db03ee6cd9b37a0f047262e697ed4e7114c41d362bbf6cc56ab75509ee787473e1bdbb80591a591472337a602e3187e4ce40d691b387dfaf48dea130100000000000080352a8ae2cab22cdf531f0eeb6b3805574b7efb5875c4976e6b273daef7cbfa47c1159daed56a6d13dbbc57440e8d9c1af966e457c5cadb13de19f96ae4c4d8261e12ed4bd3ed76ebd300000000000000a0c31545f1c6c84f5cdd83b1b0b682ab152bfe54fde60f8faf7abef0670fae2ab84a8ffb873ff4fb65d5effff87fe3c9c8a3e00a063763c68c4dbbbbbbf7ccf33c8b7c30b68b17457b6bb1f2f684f745be1bcfcf88bca3ababeb3559963dbd3e0d000000000000003a4096654f2e8ae2aec8f47a1f8c8635155ca562ab0bbe7757f5feaffcb8faee4f16570bee5c521d77e91dab0aaed2e334fcdbb7de5f1d13e37cfc8a9f578f3ea6e86a7da3e00a866d5a6c23772dcbf21fa27d6fe4acc8c2c843799eff2eda1b22e746fffba29dd56ab59e1bdbd627d52702000000000000c0145114c587cbb2fc527d388c9635155c3df2e8b2eae84b7ebcaac06a5d39f2cbb757f73df28701d391e145c1158c9ed9b3676fdbd5d5f5cad88ebe35cff3d322df8eedeaaf238f47ee88e7ff1ded47a2ed8ebcb8a7a7e729f569000000000000003089b45aadbf2c8ae2e12ccb76aaf7c1685953c155ca776e7b604061d59a72c98df70c78bd0c3f0aae60ecf5f6f66e5696e55eb18dcd23c747be1cb92db22c724fe4cae83f3ddab747fbeac8d3ead300000000000000a081f23cff46511447d787c3685a5bc155ba8d607f41d55117df5e5df4fdbbab5f3ff8fb76d2e334acbffff2db1f18f07a197e145cc1c4993b77ee8659963d3bb6bf332247c436f8ecc875a9f839f2dbc8f7239f8fbea322af8f719fe3f684935f55559b2c5ab4e8921b6eb8e17fafbefaeaf67a58c637f1b9ff79e1c2858be7cf9fff86fafc01000000000080216bb55aaf2d8ae2e759966d52ef83d1944e74d68b7e96fcf6b16ae1cf1eac8ebbf48e550555a9c0aa3e5e1ad6df9fc64daf49afad8f27434f9a1ff579044cbcb22cb78bec17dbe6b745fbb168bf135954acbc3de14f62d825c5cadb0097d1be68e6cc999bd7a741332d5ab4e82b0b172eac962e5d5a2d5fbe7cc07a59c63ee9734f9fff82050b1e8dede0ebeaf308000000000000d669c68c199b1645f1cb56ab7550bd0f46db600557f5db05a6a4ab5ad5c74bc3eae3a5d4c793a147c1154c2e59963d39cff3174662d35dcc8d5c1cb9bdaf10ebeec8e5d1f78968e7747575bd2ac6dfbe3e0d2656bab2552af6a9af8f65fcb364c99247623bf8c3fa3c02000000000080752a8ae2d8c8a5f5e13016145c352b0aae606a48b7276cb55a7f19dbf3d79565f96fd19e13b93ef29bbea4c7e7f4f5bd2e8d9b5e539f0e632fdd46d095ad9a91341f623bb8a23e8f00000000000060ad8aa2d825f27064d77a1f8c85c10aaedc5270e2a2e00aa6be7495ab74b5ab74d5abbeab5f5d5eacbc1a56ba2a56ba3a56ba4ad6dc74d5ac74f5ac7415adfa34183d836d0765e2623b080000000000c0b01545f1953ccf3f581f0e63656d279a17dcf9e0aa82aaa32ebebd5d6095ae6a95921ea761fdfd69dcfaeb65f871a2193ad7cc9933378ffd80179521da0f477349b43fe92bc45a14f94e0cfb58b46f8b76bfc876f569307c6bdb0ed6938a8afbb77b0a8cc726b6830000000000000c4b511407447edddbdbbb59bd0fc6cada4e345ff1e30706dc2e704df9d6adf70f78bd0c3f4e34037559963d29f29c3ccf5f1f392af6153e1ff97ee4b7c5caab625e17393bfa8e88cc88719fedf68443b7b6ede08a157faa7ef387c7573d4f5772ecdfeea5c7fdc31ffafdb2eaf77ffcbff164e4b11d04000000000060c8e6cc99b3715114774666d5fb602cade944f3238f2eabdeff951f0f28ac5a53de77c9eded13cef5e9c8f0e24433301c65593e2df2ead87f787bb4a7477b65e49ec8b2c86d31ec4bd11e1f6d57642f45dd03ad693b988aad2ef8de5ded6de1777fb2b85a70e79227dc6a373d4ec3bf7debfdd53131cec7aff879f5e8638aaed637b6830000000000000c595996ff96e7f9b7ebc361acade944f3b2c757549f99ffcbf66d03bf76f3bdd535773e38e044733af99c4e36a793d1ffb5f0d7d5f2152b064c478617279a81d1d0d3d3f394d8af7871a4bb288a8f44fbdfd1de51acbc3de1afd33e47e4b4d8ff786b5757d72b67cf9ebd6d7d1a9d624ddbc154787cf425432f3c3ef2cbb757f73df28701d391e1c576100000000000802139e490439e916e09946e1754ef83b1b6a613cd29a9e8eac1df3db6eaf99a6ea5f4db3f3caed86a94e244333096d2ed095badd673d31535cbb27c5fb4e7466ec8f3fc77d13e145918392bf2dee8ff8768778d974dab4f672a59db76f03bb70dfdd6ba97dc78cf80d7cbf0633b080000000000c09094657941511427d687c37858db89e67a96fcf6b1552796d3e37abfac7fa6ca89e6aaaa3659b468d12537dc70c3ff5e7df5d5edf725e39bf8dcffbc70e1c2c5f3e7cf7f437dfec060b22c7b7a5757d76bf23c7f47e48cd837f96ee4bec863f1fcd6682f8af68391acbbbb7bcf1933666c5a9fc66494be2ff575717fd2951cfbb77be98a8f177dffeeead70ffebe9df4380debefbffcf60706bc5e869f343feaf3080000000000009ea0288abf8ddc3373e6cccdeb7d301ed676a259c63f53e544f3a2458bbeb270e1c26ae9d2a5d5f2e5cb07bc4f19fba4cf3d7dfe0b162c783496abd7d5e7110c5596655bc4beca4bcbb23c24158847be1ab933b23cf2abc83723a7460e8dbca2d56a6d539f46930db61d4c45c5e94a8eabdf4a371558d5c74bc3fafbd3b8e9350a92d72f53653b080000000000c01849b7f5298ae2f674a5887a1f8c97c14e34cbc465aa9c684e57b64ac53ef5f727e39f254b963c12cbd50febf308d6d7f4e9d3378a7d99bf8afd9837448e897d9af32237461e8d2c8d2c88e19f8d1cde6ab50e8a719fb941036f4f38d876b07ebbc0947455abfa7869587dbc94fa7832f44c95ed200000000000006324cff377154571557d388ca7c14e34cbc465aa9c684eb7117465ab6624cd8758ae56d4e7118ca5d8bfd9b12ccbfda37d67e4ccb4bf13b93ff258e447910b231f88bc39c67b4196659bd4a7315e06db0ed60ba8145c8d5fa6ca7610000000000080319065d9f645513c94e7f9f3eb7d309e063bd12c1397a972a2d972d5ac4c95e58ac96fd6ac595bc6becfcb22b3633fe8a4c8a5919f152b6f4ff88b18fe8dc829917f8abcbcb7b777ebfa3446db60eb2bb7149cb8585f010000000000b04645517c3ecff3d3eac361b49465f9bbc87beac3eb063bd12c1397a972a279a8cbd563bf5f522dbae9dceaa7577db89df4380dab8f27eb97a9b25c3175cd993367e3d8663d2f7270e4fd912fc4bed20fa3fd43b44b22d7443e1df9d718f677d1eeb2c128dd9e706debab05773eb8aaa0eaa88b6f6f1758a5ab5aa5a4c769587f7f1ab7fe7a197eacaf000000000000185451147f1379205de5a1de07a32596b12a655d85576b3bd12ce39fa972a27928cbd51f7ff74075c795c755b77ffba827240d4b7df5f165e4992acb159d29cbb29d627b7640dfad983f15b93aed4745fe18b939f2c5e8fbf7c89b62dc3d225bd4a7b1366b5b5f5df1e30706dc2e704df9d6adf70f78bd0c3fd6570000000000000c3077eedc0d8ba2b8a92ccb43ea7d309afa0baed65578b5b613cd32fe992a279a87b25cddf7d36f0c28b6eacffd3ffde680f165e4992acb15acaebbbb7bab56abb54f6cdbde12f98fd8d67d3df2f3c89f23df8b61277475751d3873e6cccdebaf5ddd9ad6578f3cbaac7aff577e3ca0b06a4d79df25b7570ffd7ed980e9c8f0627d050000000000c0004551bc2d725d7d388cb67ac1d59a0aafd674a259262653e544f35096ab9f5d73ca8042abfea4befaf832f24c95e50a86225d4134b67707443e92f6b98a9557c2ba7e4d05586b5a5f2d7b7c45f599f9bf6cdf36f06b37df5b5d73e783d57197deb1aac02a3d5e70e792eabb3f59dc2eccfaaf85bfae96af5831603a32bc585f010000000000f00459963db5288a07f33c7f61bd0f465bbdd0aa9efec2ab359d689689c95439d13c94e5ea8e797307145af527f5d5c7979167aa2c573012b1fff5e4b515605d7ef9e503be33fd4945570ffeeeb155cf17feecc1550557e971fff0dffee171c556a314eb2b000000e840f513182222222222224dcf500a6364fc529f3f4349fdd8b40986b25c29b81abf286080ff532fc07acb5bde522d5b36b45b012ef9ed63ab0aaed2e37abfac7facaf000000a00335f5876e0000265eec2bbe28b2a4b7b777eb7a1f9d61bc8f17ea4539fd714bc16667b8279ac77bb91aaaa12c573fbdeac3030aadfa93faeae3cbc833dce50a3ac995575e39e03b231317eb2b000000e8404dfda11b008009372df615bf17f9e77a079d63bc8f17d65568d56f288531327e19ee89e6f15eae866a28cbd52fbff7a9018556fd497df5f165e419ee72059d6428eb2b19bf585f01000040076aea0fdd00004cac3ccf67c7bee28df1705abd8fce31dec70beb2ab4ea3711279a4f3cf1c46ac71d77ac36df7cf36aafbdf6aa2ebbecb201e33421471e79649a67d5a2458b06f48d55867ba279bc97aba11aca72f59b077e5cddf1dde307145ba561a9af3ebe8c3cc35daea0930c657d25e317eb2b000000e8404dfda11b008089d3ddddbd55ec272ecef3fc65f53e3acb781f2facabd0aadf789f68beeaaaabda454cf19da8ce3ffffceaed6f7f7b75db6db70d18af0939eaa8a3145c8dd05097abbb6e3e7f40c1551a561f4fd62fc35dae185b59963da93e8c8933d4f5958c4facaf000000a00335f5876e0000264e9ee79f88fdc4b3ebc3e93c4d3d5e18ef13cd175c7041bb88e9d4534f7dc2f05b6eb9a53dfc84134e683fbffcf2cbdbcf3ff7b9cf55f7dc734f357dfaf4f615b1b6dd76dbead0430fadeebcf3ce76ff073ef0816ab7dd76abb6d9669beaf8e38f5f35bdb3cf3ebbda75d75ddbafd96fbffdaa5b6fbdb53dbcff75279f7c72f58217bca0da628b2dd255c0aac71f7fbcdd7fecb1c7564f7dea53ab3df6d8a39a3973a682ab111ad272b56245f5cbef7f6640c1551a96fa068c2f23ce70972b46261552c57772c7c84b63bd3233f22ff1786eda0f887c2bf2a3c892f4bd9d3b77ee86f5d733bae273be2632bd3ebc6e48eb2b19b7585f01000040076aea0fdd00004c8cb22c5f10fb884ba3ddaede47e769eaf1c2789f685eb26449b5d34e3bb50b995ef5aa575537de78637bf8da0aae5271567afca10f7da83ae38c33aa4f7dea53ab0aa776d96597ea0b5ff84275c00107b49fffe0073fa86ebae9a66adab469d5c1071f5c7dfad39faef6dc73cf6aefbdf76e4fb7ff75db6db75d75ce39e7546f7ad39bdacfbffef5af57d75e7b6dfbf181071ed89e662ad84acf155c0ddfba96ab3ffeee81ea17df3b7340b1557f525f1aa7fe3a195986bb5cf144c328a4fadfc8e2bee7df8c9cd537de9cf4ba56abf592430e39e4194dfdde4e35e973eecb5a0bafd6b5be92f18df51500000074203f980100b0ba7482af2ccbc3eac3e94c4d3d5e988813cdf7dd775ff5b6b7bdadda64934daa4d37ddb45de8b4b682ab5440b5d5565b553befbc7375e28927568f3cf2c8aac2a9238e38a23dfe95575ed97e7ee69967561ffde847db8f57cf861b6e582d5bb66cd5ebdef18e77b45fd77762b75dc475da69a7b51f2f5cb8b0dd970abcd2730557c3b7c6e56ac58a6ac92fe7573fb9e2d8014556f5a471d2b8ae76b5fe19ee72d52946bb906aa8b70a6ceaf776aa499f732d83165ead717d251312eb2b000000e8407e300300a05fec1be6e1d6a19e7c65ea6beaf1c2449e68beeebaebda054def7ef7bbdbb7fc4b8f3ffce10fb7fb2ebcf0c2f6f35470959e2f5ebcb85d00b5e5965b562f79c94b56154e1d7ef8e1edfe2baeb86255e1d429a79cd27e9cae86357ffefc5559b162c5136e45985e9786a7e7a950abff4a5ad75f7f7dbb2f1577a5e70aae866fb0e56a5d57b55a535ced6afd33dce56ab29ba842aaa16aeaf776aa499ff31af284c2abc1d6573271e9b4f515000000b0811fcc000058a9a7a7e729b16f786f5757d72beb7d74aea61e2f8cf789e64f7ef293d561871d569d7beeb9ede2aaf82754a79f7e7afb5683e93680af7ce52bdb4550e97683a92f155c5d74d145d5c9279f5c5d7cf1c5d53efbec536db6d966d51d77dcd1eedf71c71dabf3ce3bafda7ffffddbafbff9e69bdb4957b4da77df7ddb855be9b682e9ea55e9efafade0ea9a6bae693f3ee8a083aa2f7ef18bd5739ef31c05572334d8723594ab5aad29e9b5f5e9c9d0b3aee52acff3d99177d587374dd30ba986aaa9dfdba9267dceeb48bbf06ab0f5954c5cd6b5be02000000a6203f98010090c47ee1c965597ea13e9cced6d4e385f13ed19c8aa6522153ba95e0d39ef6b4f615aa1e7ffcf1765fbaa2d436db6c536dbffdf6ed82a88d37deb85d70958ab3d2b0f4fc59cf7a5675d65967ad2a9c9a356b56b5db6ebb55db6ebb6d75d24927adfa3be79f7f7eb5fbeebbb75fb3c30e3bacba55e1da0aaed2f3a38f3ebada7aebadabbdf6daab3d4eea5370357c832d57f522aae1a63e3d197ad6b45ce579be5b2c4357f5159f9c55ef1f2f53a5906aa8d2e75d1fd609d2fb6e62065b5fc9c4654deb2b000000600a4b3fd2d4870100d059f23cdf3df60b1fceb2ece9f53e3a5b538f1726eb89e6fec2a9e38e3b6e40df64ce704f345bae6428a92f5773e6ccd938b657c7c4f2f3787fd1493cffc6eae38c864e2ba41aaaf479d7877582f17edfe9efad21d7146e29d8d8d4d757000000400718ef1f8e0000689e3ccfaf88fdc2f7d68743538f1726eb89e6fa95aaa64a867ba2d9722543c9eacb556ca75e1ef9f120452837af3e0fd74621d5fa499f777d582718eff79dfe5e2d4f28b4ea677dd5ac0c773b080000004c01e3fdc3110000cd5296e5c1b14f78c7f4e9d337aaf741538f1726eb896605572b59ae642849f3a3bbbb7bab585ece8cfc7f8314a2a43ca0906a7ca4cfbb3eac138cf7fb4e7faf2f83165af51b8bf5d5b265cbdadba894740bdde73def79ed5be4d6c71bad2c58b0a0baf0c20b573dbffffefbdbb7c73de59453068c3bd4d4a7395e19ee76100000009802c6fb872300009a23cbb227c7fee05d5d5d5dafa9f741d2d4e385b138d12c23cf704f345bae642839e79c7352d1492a90aa1759d5a3906a1ca4cfba3eac138cf7fb2ed65168d56f2cd657fd0557fbedb75fbb6869df7df7ada64d9b56fde8473f1a30ee68e4b5af7d6df5c637bef109c3aeb9e69a6ac9922503c61d6a069be67864b8db41000000600a18ef1f8e0000688ebe13d25fae0f877e4d3d5e188b13cd32f20cf744f3782f571359c02023cbf2e5cbd3d5ad5231d563799eff3f6999595362bc9debf392d137dedfdba668eafb1e8bf5557fc1559665ede75ffdea57dbcfcf3befbcf6f3b3cf3ebbda75d75dabcd37dfbc5d9475ebadb7ae7aede1871f5eedb8e38ed5669b6d56bdec652f7b4291d6e73ffff96ab7dd766b5f35ebb9cf7d6ef53ffff33fd5b1c71edb9e767fce3df7dc275c05f2852f7c61b5cb2ebb542b56ac684fa3a7a7a7da68a38daafbeebb6f8d7f6bb069aeebdf3d5a19ee761000000098029afac3110000632bcbb267c7bee0c34e54b3364d3d5e188b13cd32f20cf744f3782f57ab15e7acb5f0ca72d5acf42f57b1bdda24cff32ce6dd7722ff6fbde02a5dc1aa3e2f197de3fdbd6d8aa6beefb1585ff5175c1d7cf0c1d5dd77dfdd2e7a4c57b8bae9a69bda498f53dfa73ffde96acf3df7acf6de7bef55affdfad7bf5e9d75d659d5273ff9c96acb2db7ac66cc98d11e9e5eb7e1861b56af7ef5abdb57cd9a3b776ebba0f2da6bafad36de78e3ea15af7845f5b5af7dadbaebaebb9e5070f5f18f7fbcfd388df7c73ffeb17dabc1993367aef56f0d36cd75fdbb472bc3dd0e020000005340537f380200606cc57ee065799e1f531f0eab6beaf1c2589c68969167b8279ac77bb9aa17e8146b28bcb25c352b832d575996ed5496e5fb63fefda27f7ec6b6ecf5f5f1187de3fdbd6d8aa6beefb1585ff5175cad9ed34f3fbdddf7d18f7e74405f2aa44aaf59ba746975e08107b6af40d5df97ae50b5faebe6cf9f3fe0efa52b5ead7efbbfd50bae162f5e5c6db2c926d5bbdef5aeead24b2f6d0f4f57dc5adbdf1a6c9a6bfb77d7ff3deb93c1d657000000c014d7d41f8e0000183b799ecf88fdc09fa7ab86d4fb60754d3d5e188b13cd32f20cf744f3782f5783145c0d5a7865b96a56d6b55c9565b95fccbfcfc736adbbdec7e81befef6d5334f57d8fc5faaabfe0ea35af794df5d9cf7eb6fd385d692af59d72ca29ede7679c7146bb78aa3fe9967ffd454def79cf7bdadfdb74fbbebdf6daabfdbad34e3baddd77cd35d70cf87bf5e2a8d50baed2f3d4b7d34e3b556f79cb5baa1d76d8a1fdef5bdbdf1a6c9a6bfb77d7ff3deb9375adaf0000008029a8a93f1c01003036529155ec03fea22ccb7fa8f7415d538f17c6e244b38c3cc33dd13cdecbd5208556f5b40baf2c57cdca70972bc6d6787f6f9ba2a9ef7b2cd657fd0557b1afd87e7ee8a187b60b986ebcf1c6eae69b6f6e5f196adf7df76ddf1a30dd9e2f1553a5f1fa8b9a8e3efae8ead4534f6d5f7daabf08eafaebaf6ff74d9f3ebdbae8a28baa638e39a6baf7de7bdb7ddb6db75db5db6ebb55175f7c7175cb2db70c28b8baecb2cbdacfb7df7efbea88238e58e7df1a6c9a6bfb778f66acaf000000a00335f587230000c646ba15539ee75fab0f87c134f578612c4e34cbc833dc13cd83143c352296ab6665b8cb15632b7d47eac33a4153dff758acafea05578f3cf248f5cc673eb37aee739f5bfde637bfa9ce3ffffc6af7dd77af36de78e3f615a74e38e184f678f7df7f7fb5cf3efbb48bb30e3ae8a0eaf0c30f7f4211d4273ef189f674527f1abe64c992f6f074d5a9a73ef5a9d5565b6dd52ec6aa175c3dfef8e3d5339ef18cf6b0db6fbf7d487fab3ecd346c4dffeed18cf51500000074a0a6fe700400c0e88b7dbf5d220f4776adf7c1609a7abc3016279a65e469fa89e67a61d56ab9a6704bc1c6a6e9cb55a769eaf660ac35f57d5b5f352bd657000000d0819afac3110000a3af2ccb4b62ffeff8fa705893a61e2f38d1dcac34fd44f3ba0aadfa59ae9a95a62f579da6a9db83b1d6d4f76d7dd5ac585f01000040076aea0f4700008caeb22cf78f7dbf45bdbdbd9bd5fb604d9a7abce04473b3d2f413cdeb2ab4ea3756cbd589279e58edb8e38ed5e69b6fdebef5d565975d36609c26e4c8238f4cf3b15ab468d180beb565a4af5b579abe5c759aa66e0fc65a53dff758adaf6464b1be020000800ed4d41f8e0000183dd3a74fdfa82ccb9fe679fe867a1fac4d538f179c686e569a7ea2795d8556fdc662b9baeaaaabdac548b1feadce3ffffceaed6f7f7b75db6db70d18af0939eaa8a346543835d2d7ad2b4d5fae3a4d53b70763ada9ef7b2cd65732f2585f01000040076aea0f4700008c9e3ccf8f88fdbeefd487c3ba34f578c189e66665aa9c681e8be5ea820b2e6817239d7aeaa94f187ecb2db7b4879f70c209ede7975f7e79fbf9e73ef7b9ea9e7beea9a64f9fdebe22d6b6db6e5b1d7ae8a1d59d77ded9eeffc0073e50edb6db6ed536db6c531d7ffcf1aba677f6d96757bbeeba6bfb35fbedb75f75ebadb7b687f7bfeee4934fae5ef08217545b6cb145559665f5f8e38fb7fb8f3df6d8eaa94f7d6ab5c71e7b543367ce7c42e1d49aa6b9aed78d56a6ca72355534757b30d69afabec7627d25238ff51500000074a0a6fe700400c0e8c8b2ece9b1cff770abd57a6ebd0fd6a5a9c70b4e34372b53e544f3582c574b962ca976da69a77641d2ab5ef5aaeac61b6f6c0f5f5bc1552ace4a8f3ff4a10f55679c7146f5a94f7d6a55e1d42ebbec527de10b5fa80e38e080f6f31ffce007d54d37dd544d9b36ad3af8e083ab4f7ffad3d59e7bee59edbdf7deede9f6bf6ebbedb6abce39e79cea4d6f7a53fbf9d7bffef5eada6baf6d3f3ef0c003dbd34cc555e9792a9c5adb34d7f6bafafb5f9f4c95e56aaa68eaf660ac35f57d8fc5fa4a461eeb2b000000e8404dfde1080080d111fb7b5f8c9c541f0e43d1d4e385abafbefacfcb972f1f70c253c63f311f16cf9b376f457d1e4d466355c070df7df7556f7bdbdbaa4d36d9a4da74d34ddb054b6b2bb84ac54e5b6db555b5f3ce3b57279e7862f5c8238fac2a9c3ae28823dae35f79e595ede7679e7966f5d18f7eb4fd78f56cb8e186d5b265cb56bdee1def7847fb757d4501ed22aed34e3badfd78e1c285edbe54e0959ea7c2a9b54d736dafabbff7f549df7ba4219aba3d186b4d7ddf63b5be929145c11500000074a0a6fe700400c0fa2bcb72bfd8dfbbb7a7a7e729f53e188aa61e2f2c5cb870f1d2a54b079cf094f1cf3df7dcf3e579f3e6fdb03e8f26a3b12e60b8eebaebda8549ef7ef7bbdbb7e74b8f3ffce10fb7fb2ebcf0c2f6f35470959e2f5ebcb85dc8b4e5965b562f79c94b56154e1d7ef8e1edfe2baeb8a2fd3c154e9d72ca29edc7e96a58f3e7cf5f95152b563ce15684e97569787a9e0ab5faafa475fdf5d7b7fb5271577a9e0aa7d636cdb5bdaefe9e479a74cbc3d87655b10eba2372519ee7c7b45aadd7767777efdc9e598cbba66e0fc65a53dff758afaf64785170050000001da8a93f1c0100b07eb22c7b52ecebdd56966557bd0f86aaa9c70bf3e7cf7fc382050b1e5db264c923ae74353189cf7dc9dd77df7dd1bc79f3ee8dbcae3e8f26a3b12860f8e4273f591d76d861d5b9e79edb2eae8a3f539d7efae9ed5b0da65bf6bdf295af6c1732a5db0da6be547075d1451755279f7c7275f1c51757fbecb34fb5d9669b5577dc7147bb7fc71d77acce3befbc6afffdf76fbffee69b6f6e275d7d6adf7df76d176ea55b00a6ab50a5bfbfb682ab6baeb9a6fdf8a0830eaabef8c52f56cf79ce7356154ead6d9a6b7b5dfdfdaf4fd215aef23c7f61ac877a22a746ae8c2c89fc36b220face886ddc5b5badd63e3367cedc7cd58c644c34757b30d69afabec7627d25238f822b000000e8404dfde1080080f513fb79ef8c5c5d1f0ec3d1e4e38554e493aeac14f9533ad129e39ef4b9a7cf7f4a145b25e97dd54fa2af6f52d1542a484ab7127cdad39ed6be4255ba7a53ea4b5786da669b6daaedb7dfbe5d10b5f1c61bb70bae5271561a969e3feb59cfaace3aebac558553b366cdaa76db6db76adb6db7ad4e3ae9a4557fe7fcf3cfaf76df7df7f66b76d8618755b72a5c5bc1557a7ef4d147575b6fbd75b5d75e7bb5c7497dfd85536b9ae6ba5e375a49f3e30933a84f9665dbc7bae980c87b23e7467e145916f9459ee7ff1df9603c7e63b4bbc5e8d3eaaf67649abc3d184b4d7ddf63b1be9291674deb2b000000600a6bea0f4700008c5cdfc9e887a2dda3de07c3e178814ed2e40286fec2a9e38e3b6e40df54cd700a18d2551df33c7f7eacb3f2c889916f46ee29cbf20fd1de1039ab585988fcb7bdbdbd5bd75fcfba75eaf6a0a9efbbc9ebab4ecc70d657000000c014d1d41f8e000018b9d8c73ba72ccb8fd587c370395ea09334b980a17ea5aa4ec86814306459f617b13ddc2f72589ee79f8d75daf7fb8ab0eee92bca4ac559792ad64a455bf5d7f37f3a757bd0d4f7dde4f555276634d657000000c024d3d41f8e0000189956abb54fece33dd0ddddbd55bd0f86cbf1029da4c9050c0aae46d5b4d856fe65599607a7db0fa6db10c6baee17c5cadb12a6db13a6db14a6db151e90ae18597f71a7ead4ed4153df7793d7579d98315c5f010000004dd5d41f8e0000189169b17ff7c3484fbd0346c2f1029d440143b332de050c3367cedc3c152d9765f9d63ccfcf88f5df82c86f224b22df8d61a74566c7387b6759b649fdf5535da76e0f9afabeadaf9a95f15e5f010000000dd0d41f8e000018be749238f6efaeaf0f879172bc402751c0d0ac34a580a1bbbb7be756abf5da581f1e1db930f293b46e9c3e7dfa46f571a7b24edd1e34f57d5b5f352b4d595f01000000e3a8a93f1c0100303cad566b9bd8b77b305d79a3de0723e578814ea280a1596972014327ae1b3bf13d274d7ddfd657cd4a93d757000000c01869ea0f4700000c4f9ee7ff19fb769faa0f87f5e178814ea280a1596972014327ae1b577fcf3d3d3d4f59bd6f2a6beabcbefaeaabffbc7cf9f201df1b19ffc47c581cebab15f579040000004c714dfde1080080a14b57b5eabbbad536f53e581f8e17e8240aae9a150557cdd2ff9ea39d1eb96be6cc999bd7c7998a9a3aaf172e5cb878e9d2a503be3732feb9e79e7bbe1cebab1fd6e71100000030c535f587230000866c5aecd35d5f96e55beb1db0be1c2fd049145c352b0aae9a25bde7be62ab87629fe3d5f5fea9aaa9f37afefcf96f58b060c1a34b962c79c495ae2626f1b92fb9fbeebb2f8a75d5bd91d7d5e71100000030c535f5872300008626f6e77a22e97fd54fabf7c1fa72bc40275170d5ac28b86a96be82ab8e2ab64a9a3caf53914fbab252e44fe9fb22e39ef4b9a7cf5fb11500000074a226ff700400c0da7577776f15fb730fe479feb27a1f8c06c70b74927402bd5ef4231397343feaf3a8293a6dddd87765abaad38aad924e9bd7000000000c911f8e000026afb22c3f96e7f9e7eac361b4385ea09328b86a56145c35435fb1d5439df49e57d7a9ef1b0000008075f0c31100c0e49465d91ee904685996dbd5fb60b4385ea09328b86a56145c4dbcfe62ab7465ab4e79cf759dfabe0100000058073f1c01004c4eb11f7775e49df5e1309a1c2fd049145c352b0aae26d6eac5567dcfa7fc7b1e4ca7be6f00000000d6c10f470000934f59965db11f775b96654faaf7c16872bc40275170d5ac28b89a383d3d3d4f89f778577fb15532d5dff39a74eafb06000000601dfc70040030b9f49d04bdb72ccbfdea7d30da1c2fd049145c352b0aae26d6cc9933375ffd7927bce7c174eafb06000000601dfc70040030b9c4fedb4991f3ebc3612c385ea0935c7df5d57f5ebe7cf980c21f19ffc47c583c6fdebc15f579d4149db86eecc4f79c74eafb06000000601dfc7004003079b45aade7c6fedbc359963dbdde0763c1f1029d64e1c2858b972e5d3aa0f847c63ff7dc73cf97e7cd9bf7c3fa3c6a8a4e5c3776e27b4e3af57d03000000b00e7e380200983c62dfedf23ccf8fa80f87b1e278814e327ffefc372c58b0e0d1254b963ce24a571393f8dc97dc7df7dd17cd9b37efdec8ebeaf3a8293a71ddd889ef39e9d4f70d000000c03af8e108006072c8f3fc0d6559fe74faf4e91bd5fb60ac385ea0d3a4229f7465a5c89f22958c7bd2e79e3effc6165b251db86e7c75df7b7e75bd63aaebc0790d000000c050f8e10800a0f97a7b7b378bfdb6456559ee5fef83b1e4780160a00e5b37a622aba59123fbda8e2abaeab0790d000000c050f9e10800a0f9629fedf8c8c5f5e130d61c2f000cd441ebc6fe62abfe22abfaf329af83e63500000000c3e187230080668bfdb55d230f777777ef5cef83b1e6780160a00e5937aea9b86a4dc3a7a40e99d7000000000c971f8e00009a2df6d7be5e96e5fbebc3613c385e0018a803d68deb2aaa5a57ff94d101f31a0000008091f0c3110040739565f90fb1bff68b2ccb36a9f7c17870bc0030d0145f370eb5986aa8e34d6a537c5e0300000030527e38020068a65464958aadf23c9f51ef83f1e2780160a029bc6e1c6e11d570c79f74a6f0bc06000000607df8e10800a099f23c3f26f6d52eab0f87f1e4780160a029ba6e1c69f1d4485f37294cd1790d000000c0faf2c3110040f3747777ef1cfb690f6759f6ec7a1f8c27c70b00034dc175e3fa164dadefeb1b6b0ace6b0000000046831f8e00009a27f6d12e8eccad0f87f1e6780160a029b66e1cad62a9d19a4ea34cb1790d000000c068f1c3110040b3747575bd26f6d1eecab2ecc9f53e186f8e1700069a42ebc6d12e921aede94db82934af01000000184d7e380200688ee9d3a76f14fb677794657970bd0f2682e3058081a6c8bab15e1c352df296be7638eaafab4f77529b22f31a00000080d1e687230080e6887db3f7e6797e457d38f4ebededddba3e6c2c395e0018680aac1beb4551a958eaf391f4be523bd4a2ab35bdae3efd496b0acc6b00000000c6821f8e00009a21cbb2a7c7bed9c3799eef5eef83a4bbbbfb805846febf5846ee88f68b6559fe5b5757d781d16e571f77b4385e00186892af1b072b867acb062b8ba6fa3394a2abd58badfa337bb5fec1fecea433c9e7350000000063c50f470000cd10fb65e79765f91ff5e190c4b2b15f2c230fa502ab685f94e7f93fc5b0d3e3f18278fcbb681f88f6dbd19e18c3bb52e1dedcb97337ac4f67b81c2f000c3489d78d6b2a821aac786a6d4557838d7f4edff0d5ade9ef4d1a93785e030000003096fc70040030f1bababa5e19fb65f7f6f4f43ca5de07b16cbc22b2347240bdaf5fabd57a56f4cfcaf3fc83d15e1af975e48f911b62d867a37d7bb42f1fee32e6780160a0895c37c6dfbe2632bd3e7c08d655fc345811d5604557838d3758b155bf75fddd469bc8790d0000004083f9e1080060626559f6a43ccf6f8dfdb2bcde07a9482a968da56559fe5dbd6f5dbabbbbb74a57c68a69bc2bf2b998ce4d9165919fc7f04ba23d36f2ba580677aabfb69fe30580812672dd98fe765f86537835d4a2a7c18aa9562fba1aac7f6dc556fd86faf71b6722e735000000000de687230080895596e56191f9f5e1d06ab5f649c556d11e54ef1ba954e017d923a6dbcaf3fc9468af8c3c18793872550c3b2dda9eeeeeee3de7cc99b3b1e30580812672ddb85ac1d5500baf865bec345851557a9e6e535b1f3e9462ab7ec3fd7734c244ce6b000000001acc0f47000013a72ccbed8a95572f7a41bd8fce16cbc54bd3b2d16ab55e5bef1b0b59963d3d1576c5df3c3a96c72f457b67b1f26a58e964feb9799e1f1ec35fdddbdbbb75fdb5009d66227f4ba9155badabf06aa4454e83155dfdbcf67c38c556fd46faef99301339af0100000068303f1c01004c9cbedbbc7dbc3e9cce16cbc58b63b97830dad7d7fbc65396654fee3b89ffb6c8a722d7471e8ddc1db9ac2ccb0fc5bff14dad56eb2f3718fe497780496b227f4b59adc06a4de92fbc5adfe2a6c18aaed6a7d8aadffafebbc6d544ce6b000000001acc0f4700001323cff397c5bed803dddddd5bd5fbe85cad566bef6265b1d51bea7d136190e38569f16fdbad2ccb7f8cbe0fc7e36f447b4fb1b210ebbac899d1f7d6685fdadbdbbb59edb5005342adc0a971c9b2acda6cb3cdd2fafbc8fabf7d98d26d04eb57b64acfd3f0f57164fddf2c223215525fd90100003085391004009810d3623fecc63ccf67d73be85c6559ee15cbc592c81beb7d1365a8c70bad566b9b18777abc87f7447b5e2cdbb746fb783cff69b41745fbbe18f6f7d13eadfe5a0086ae7e727fb55c93d6c3ab8dbabe57925adb15aed2f08eb8c215c0500d75bf1900008029c2812000c0f88b7db07f8e7c6f83919fac648ac9f3fcaf6399581c7973bd6f22adcff1c29c3973364e4564a9b030da8fc5b4ae8e3c125912c3ae88f6e4185ec6e3e76759f6a4faeb011868b502ab35155aad6ea4c54d83155bd5af743592a2ab91fe7b001a6f7df69b01000098841c0802008cafdededead53c149e445f53e3a5396657b142b8badf27adf441b8be385eeeeee9df33c7f7de4df63fa5f89fc22f258e48791b323efeceaea7ae5ac59b3b6acbf16a0d3f51559adabd06a75c32d721aacd8ea9c0d56de46b03e7c384557c3fd77004c2a63b1df0c0000408339100400185fb1ff75669ee7ff591f4e672acbf279b14c3c90aef454ef6b82f13a5ec8b26c8bf808f68defc63be26f9e15f9415f11d6af225f8d1c17fd3363bc67d65f0bd049865168b5baa1163bada9d8aabfa86ab0fea1145d0df5ef034c5ae3b5df0c0000404338100400183f799ebf30f6bf1e6cb55adbd4fbe83cb13cec1ecbc3fd65591e52ef6b8a893c5e983b77ee865996fd55fc1bf2c84991ef142baf04f6db5470109fdf27a2ed8defd3de31de26f5d703f004eb2a7a1aac986af562ab7e838db7b6a2ab75fd5d80296122f79b01000098000e040100c64fec7b5d17795b7d389da7d56a3d379685fbcab27c4bbdaf499a78bc9065d9f6f1b9fd5dfcdb8e8cf682687f12793c727b3cff429ee74744bbffecd9b3b7adbf16a0c3ada9f869b022aac18aadfa0d36fe6045576bfa7b00534e13f79b01000018430e040100c647ba8a51ec7bdd94aeda53efa3b3e479be5b2c0bf7460eadf735cd64395e983163c6a6f1b9be387da6d19e11dfb76be3f1ef53515be45bf1fc8468df9c0add3618581000d049062b827acb06432fb6ea3758d1d5ecd5fa07fb3b0053d664d96f060000609438100400187bb366cdda32f6bb1e68b55afbd4fbe82c59963d3b96857b8a4972a5b3c97ebc10fffe5dcbb23c38dae3f33cff5ab477c5f33f44fbfdc8a7e3f1bf44fb373367cedcbcfe5af8ffd9bb13f828eafbffe31c227896e255b55aeb5d5badfdd5daeacf4a6c3d1a6da8d64e26b3213178e051af2aa8f527365eadd7dfbbd4ab1e2854a5a2b6968a1188f1c0030f10518b020a122278811c6dadf3ff7c872c0d9f3db2bbd9d9f9ceeeebf978bc1f9b9defccee32df39773ecc02654c1743752d9ecaa5d82a29d374faf501a0ecc5fdb8190000000090274e04010000c2e7baeed572dc652e44a28299e29fce829f13759badcaf17cc1719c2fd5d6d61e28fd709afcfbfe28992e5921cfdf94c7fbe4f1d7b2ce56cbdfdbe86901a08ce8a228532c65ee50956bb155929e4ebf2e005484723c6e060000000064c18920000040b81cc7d9438eb93e90c72d741b2a87f4fff6b21cccf53cef14dd66b34a395fa8aaaa5a4ffae69baeebd6cbbff92ac9e366bd3591e12d669869937ccb8caba70780982a767154b15f0f0062a3528e9b01000000009d381104000008971c6f3de1baeea97a382a477d7dfd57653978dbdc5149b7d9aed2cf17860e1dbab5b9db95e4bcba3577bf7a531e57485eaa5b7377acd3cdddb2cc5db3f4b4001013c52a922ad6eb00402c55fa713300000000541c4e04010000c2e3baae23c75baf3a8ed357b7a13248df6f2bcbc01ccff3ced46d71c0f942aa9a9a9a0d65be7c5f325c325af28cf4ef32799c27ebfc43f2d82ccf8f94beffba9e16002cd5d362a99e4e0f00b1c771330000000054185b4f047ddf5f7feedcb90f4c9b36ed5f93274ff65b5a5a488923f3fd8bb6b6b64553a64cf999ee1f5bb1dc449f382e370807eb63f4617d8c5e6751c6bb921fea365406737724e9ffb72423745b5cd87abe60a1de8ee3ec2cf3eb179ee75d2a8f7f952c907c2269735df706793c3691487cb7babababf9e18002c5068d154a1d3014059e1b819000000002a8cad278273e7ce1ddfd6d6e6777474f8ab56adf2fff9cf7f9212c7cc7733ff5b5b5b3f6d69693942f7918d586ea24f1c971b8483f531fab03e464f8eb32e93dca387a332388ef395ce9f9f3b57b7c589ade70b71d1d8d8b8992c073f92f9f82b79bc5b1e6748564a66c9f37b5dd71d298f873434346ca9a7058008e45b3c95eff80050b6386e06000000800a63eb89a0b9238ab948ac2f1e93d2a7bdbd7d694b4bcb0bba8f6cc472634fe2b4dc201cac8ff684f5311a9d77ba5962ee70a4db50fe3ccfdb4afa7fb6e47cdd1637b69e2fc4996c1fd64f24127bcb72728cebbad7cae31499cf1f4a164926caf3dfc9a32b8fbb373737f7d1d30340c8722da2ca753c00a8081c37030000004085b1f544d0fcfc147744b123a61f5a5a5a56eb3eb211cb8d3d89d3728370b03eda13d6c76874164d9cad87a3fc398eb385b97b9164946e8b235bcf17ca91ccebed64bb5163961dd775ff2c8f6f4b3e933c2fb945869d2c8ffbcb32b6b19e16008aacbb62aaeeda01a0e270dc0c0000000015c6d613c1969696940bc624ba98fed07d6423961bbb1297e506e1607db42bac8fa525c7574324b3870f1fde4fb7a1bc799eb7b9ebba33a5ff9b755b5cd97abe5029860c19b2496d6dedffcab2758af4c5ad92173a8bb0e648c64b2e9065eea7f5f5f55fd5d302400f652aaaca341c002a1ac7cd0000000050616c3d11cce7427dfb479ff967fde9d520e66fdd4e7a9eb85ca8cf75b9f9ec93767fee8b77faaf3f717110f3b719a6c7233d4b5c961b84a3bbf5f1c57796f8e73e3073edf63b53ae9af8a6bfe8c3e529d393fcc2fa583a4d4d4d03e4f8ea1dc98f751bca5b6363e366d2efaf4a2ed16d7166ebf94225731ca7afebbadf9048f7d45d2ef9bba4bd6ecdcf124e965c236d8d926f53f809a0877471957e0e00e8c471330000000054185b4f04b35da85fbdfa9ffe87cb56ae7ddef6c6e2b517e6cddfc9e11f7cb2c2ff64f97fc72385272e17eab32d37c92cfff87d7fd6a451fe8cbf8d5c27669869d3e393c21397e506e1c8b63e9e377ea6fffcdb1ff82db316a51458a5cb2d53df4e790d925f581f4bc775dd0be5f86abc1e8ef2964824be2cfdfeb2e779bfd36d7167ebf902523534346c29dba04365393c47fa6dac3cbe2e8f2bebd61402de25cfcfacadad3dc8719c417a5a00c822596435a2f391622b004883e36600000000a830b69e0866ba506f8aadee7d669effebf133fdc75f5be4b7ce6ef7474d98b5f6a2bcf9db0cffdb2b0b830bfad73cf696ffe967145df53471b9509f69b9e99a05afff25a5d82a9985afff35657c5278e2b2dc201cd9d6c7e49dab162e5d9e525c952e174e782de535487e617d2c0d39aeda41b244b29d6e43f96a6a6a1a287d3eddf3bc2b755b39b0f57c01b9e9bcebde3e92e324374a9e927c2a79cf75ddbfc8e325b2ec1e2d7fef24a3f7d6d3034027ee6c0500dde0b819000000002a8cad2782992ed42ffd74454e3f4195cc88fb66f80b962e4b791d925fe272a13ed372d3356f4cbd22a5d02a19d3a6c72785272ecb0dc2916d7d34dbe791f7cf48194ec20beb6369b8aefb901c5b9daf87a37c398ef325e9f3173ccffb7fbaad5cd87abe801ee99d48247694be3d4a96dd8be4f161c97cf97b993c3e2ddbb2dfcbe30932cebeb28c6fa0270650b136d4030000ffc571330000000054185b4f04b35da89ff8eafb29855599f2c0f3efa64c4ff24f5c2ed4675b6e9299d5d29c5268958c69d3e393c21397e526021571a122dbfaa8b7d5d9a2a7258585f5317c8944e23039ae9a535d5ddd5fb7a13cd5d7d76f2a7dfe9ce779d7e9b67262ebf9028acfdcad4d96e7c1aeeb9e218f7748dfbf2c592179439eff491ecf95c79f388ef3153d2d000040a5e3b819000000002a8cad2782d92ed49b9f114c5e8837774819f7ec7cff9dc59f04317f9b61c9f6bfcf783f657a927fe272a13edb72930c0557a54b5c969b121bdcab427e8a23dbfaa88baab2454f4b0a0beb63b81cc7595f8ea9de4a241287eb3694a72143866c227dfeace446dd566e6c3d5f4069545555ad575f5fbfa7e779435dd7bd5a968727ead6fc746a876492f9294d794cc876700f495f3d3d000040a5e0b819000000002a8cad2782e92ed4b77ff499dff6c6627fd484596b2fc49b022b3d9e19966c37e39a69ccb47a3c927be272a13edd72a3f3fa1317a7145a2563daf4f8a4f0c465b929a164b1d588cec7b22ebacab63eeaa2aa6cd1d392c2c2fa182e73d717d775ffa287a33c398eb3b1f4f9d392d1baad1cd97abe8068c97ab0ad293295e5e37cc9fd92b7ead6dc0deb45c9edb24d3c551e7f68ee04a7a705000028471c37030000004085b1f54430dd857a7d11dec4dcd54a8f6786e9f1b868dfb3c4e5427dbae54667ce3337a5145a2563daf4f8a4f0c465b929117d672bfdbcec645b1ff5f6395bf4b4a4b0b03e86c7141dc8f1d4924422b1a36e43f9696868d848fabb4d728b3cedaddbcb91ade70bb08f593f5cd7fd812c332749fe209926592e992b99206d174a7e26dbcbafe969010000e28ee36600000000a830b69e08a6bb50af2fc29b5070559ac4e5427dbae546e7c3f767fab31eff4d4ab1951966daf4f8a4f0c465b929814cc555998697856ceba3de3e678b9e961416d6c7f0c8b1d47d9ee75da487a3fcd4d4d46c28fd3d55727baf0a29b6326c3d5f403c343737f7715d77578923cbd265f2f837795c288f1fcb63abe47af97b983c7ea7babababf9e1e0000202e386e06000000800a63eb8960ba0bf5fca4607489cb85fa74cb4dbacc9b3e26a5e0ca0cd3e3919e252ecb4dc8ba2baaeaae3db6b2ad8fbaa82a5bf4b4a4b0b03e86438ea3aa24f31cc7d940b7a1bc983e96be9eec79de1dbd2aa8d8cab0f57c01f126ebd2e6b26cfd581ecf96c731aeebce94c7959d8ff798e1b5b5b5079bf1f4b400000036e2b819000000002a8cad2782d92ed4b7ce5ebcf642fcc8fb67040556e6ae5626e66f332cd96ec6d5d393fc13970bf5d9969bb559bdda9ff3ec1f520aaecc30d396323e293871596e42946b3155aee3c54ab6f5511755658b9e961416d6c7e2abaaaa5a4f8ea366498ed26d282f4d4d4d033ccf6b91dc6deed6a3dbcb9dade70b283f8ee3ac6fee7465ee7825ebdb75f2776be79db0deefbc33d66532bcd6dc31ab12d745000060378e9b01000000a0c2d87a2298ed42fd6333df4fb9209f298fbeb230657a927fe272a13edb7263b2fce3f7fd7f3c73634ab15532a6cd8ca3a72385252ecb4d48f22da2ca777ceba55b1f2f9cf05aca763a5bccf8fa354861a9f0f531149ee79d29c75193f4709417f31367aeeb3e26fd7d6fa51678d87abe80ca914824be26cbe11059172f94c709927724cb25d364d8cdf278923cfea0a1a161233d2d000040a970dc0c0000000015c6d613c17417ea4d967ebac2fff5f8992917e533e59c0766f81f7cb222e575487e89cb85fa4ccb8db97355fb9c29fe6b8f9d9f5264a563c631e372b7ab9e272ecb4d080a2d9e2a743a2ba55b1f6f99fa76ca763a5bccf8fa354861a9e0f531149ee76d25c7504b1cc7d94db7a17c98622be9e789d2df7f92beeeabdb2b85ade70ba86cf5f5f59bcaba7980ebbaa74a6e93e5f445c90ac95b32fc01793c5f7284acbbdbea69010000c2c071330000000054185b4f04d35da83759b172b5ff872973829f0d7c68fa7bfed4d98bfd511366adbd386ffe6e9dddee3ffedaa2a030eb8eb677fc5514cef43871b9509f6eb9e9eeae5699c2ddae7a9eb82c3745d6d3a2a99e4e6f8d74ebe3a20f97fb574d7c33a5b02a5dcc78667cfd1aa4b054e8fa181af3d372720c75b91e8ef2d1f9d3667f95dc5fc9c55686ade70b8066d655c91eb2cc265cd7bd421e2749164b96489e906157cb63437d7dfd9ec3870fefa7a7070000e8098e9b01000000a0c2d87a2298ee427d32a6e86af1c79fad7ddef6c6e2b517e8cddfc9e11f2d5b49b1559112970bf5e9969b5cee6a9529665afd7a24f7c465b929a262154b15eb7522956e7d24d1a502d7c7d0c8b1d3fe92058ee36cacdb501e4c2186f4f123aeebfeb9aaaa6a3ddd5e696c3d5f007225dbebaf241289c364593ed7dcb14e1e67d7adb91bd6cb923b655d3f43860f6e6a6a1aa8a7050000c815c7cd0000000050616c3d11cce7427dfb479fad2db8327feb76d2f3c4e5427dbae5461751e51bfd7a24f7c465b9299262174915fbf54a2eddfa48a24b85ad8fa1696e6eee632ed0bbae5ba7db501e4c8195f4f104e9e387b803ce1ab69e2f003de138ce06b29e7f4f96ef132437499e967c2a992f79c4f3bc8ba4fde78944624719bdb79e1e000040e3b819000000002a8cad27825ca8b72b71b950cf726357e2b2dc14812e8e3217e58ee97ccc879e4ebf6eacb03eda950a5a1f43e5baeec972ec34550f477930c556e6ae56a6d88262abffb2f57c0108416fd906ece479ded1b2dc5f2c7fff451edfed2cc47a4a72a3b41d2f8ffb3435350dd013030080cac671330000000054185b4f04b9506f57e272a19ee5c6aec465b9e9215d14658aa5fe2831ff76f3986bd155a6e9f4ebc706eba35da990f531548d8d8d9bc9715387ebbadfd26d883fc771fa4affde2f7954fe5e5fb757325bcf1780524924125f96f5a0caf3bc33e5f12ed90fbc228f2be5f9ebf2384e1ecf916187cae3567a5a00005039386e06000000800a63eb892017eaed4a5c2ed4b3dcd895b82c373d90ae18ea985e6b8aa692c9a5e8aa6bb155328d5ddad3bd8ff5581fed4a05ac8fa19363a65b5cd7bd560f47fc75165b8d934cacaeaeeeafdb2b9dade70b4094cc5df03ccfdb4bf60b8df2f8ff643d992c592a6997618fc9e3e532dc93bfbf61b6317a7a0000507e386e06000000800a63eb892017eaed4a5c2ed4b3dcd895b82c3705ca540495ae782a5bd155baf16fef1cde55a6f7b316eba35d29f3f5317472bcb48f6491e3385fd26d88b7e6e6e63ed2b7f74826516c959eade70b808deaebebbfeabaee4f25ff27ebce78c93f249f495e90dc2af9656d6dedff0e193264133d2d000088378e9b01000000a0c2d87a22c8857abb12f5857a594ea74aaaf4708de5c6ae74b7dc983e357dab87c74077c54fe98aa8d2155da51b2f5db1555277ef5b12ac8ff14c77eb23b2ea2dcbfc739ee71da31b106fa6d84afaf56e494b5353d300dd8e356c3d5f00e2c2719c8d653bb39febba27cbfa748bd9a7741661bd2d7950324ada6b64bcedf5b40000203e386e06000000800a63eb892017eaed4ad417eacd72da99ac851e2c377625d37263fab0b32f837ed5ed96cbb5e8295d3155d7a2ab74edd98aad92727dffd0b03ec63399d647744f96f36325cff6ea7efd44bcf4f63cef0ee9dbc98ee36ca01bf15f31dc5703d633059fb2edd94dd62f57f25bc944c922c947e618cbfc84ad3c36251289bd65bcf5f5f40000c03e1c37030000004085b1f544900bf57625ea0bf59dc51d5d93b6d083e5c6aee8e5c6f45967dfadd39f5dc7b15cbec54ee98aaaccf33e6986e7526c9594efe7282add7fac8ff1885e1f919ba6a6a681b27cb74bbea3db106bbd5dd7bd4dfab5b5a6a66643dd8875c56c5f0dc49ae3385b789e7788ac7723e4f15e797c4db25232c3dc914fb65d67c9e38f1a1b1b37d3d3c25ebeefaf3f77eedc07a64d9bf6afc9932707c765a4b491f9fe455b5bdba22953a6fc4cf70f00140bc7cd0000000050616c3d11345f88e98bc524ba98fed07d544a690a3cd2167ab0dcd895e47263faa8b3af74ff0559dbd1762bb4c8295dd1d55bea793ec55649857e9e1ed3fdc7fa188f44bd1d8f2bd7756f90e57ab41e8e5833c556374bbfb63534346ca41b912a46fb6aa02c555757f797edd6ffc8ba78acd92f799ef7a4fcfd896481e451797ea93cfe229148ecd22bff634a94c0dcb973c7b7b5b5f91d1d1dfeaa55ab528ed348f831f3ddccffd6d6d64fe5b8f808dd4700500c1c37030000004085b1f544900bf57625ea0bf5aaa8235d82420f961bbb3266cc9864dfe8fe5a27babf2dd4d3e2a67445573d29b64aeae9e72a88eebf34317dcefa6859a2de8ec791e7797bc9b2bcd8719c41ba0df1e5baeeefa55f9f967edd58b721bd98ecab818a23ebe60eb2af3a521e7f23dbb687e4719e3c5f268fcf4a46cbdf27cae3f7b9935ff4cc9dad4cb18f3e3e23a54f7b7bfb52392e7e41f711001403c7cd0000000050616c3d11e442bd5d51c514d686e5c6aee8fe897364b334426fa7f2647e4650dfd9ca3c37c37b6284feacb684f5d1ae5070953f598edb24c3f570c457e71dcb9e1d3264c826ba0d99996dba1e06c04e8ee37ca9b6b6f640cff34e9375f78f92e99215f2fc4d79bc4f1e7f2ddbc26af97b1b3d2dc2637e46903b5bd911d30f725cbc5af711001403c7cd0000000050616c3d11e442bd5d89fa42bd2ee4e892a975fc8499b5492e37a68f3afb4af75f90b51d6d2f7307a99edc492adb1daeccf072b9c315eba3c5897a3b1e37aeebd6cbf23cbdb9b9b9a74591b084f4e9b5d2a7cfd5d7d76faadb905d4cf6d50032a8aaaa5acff3bc6f76eedbae923c2ef9c04486b79861a64df22d33ae9e1e3dc771b15de1b8184058386e06000000800a63eb89205f48da95a8bf90ecaeb02389e5c6aee8e5c6f45967dfc5ade0ca28b4b8295db195bed355214557857e9e1ed3fdc7fa188fe8f5119999bb1fc932bd50f27ddd8678725df76ae9cf17cd9d5f741bba17a37d35803c0c1d3a746b73b72bc979756bee7ef5a63cae90bc54b7e6ee58a79bbb65b1edecb9ee8e8b5f7c67897fee0333fdb3fef46ad65c35f14d7fd187cb53a627f985e3620061e1b819000000002a8cad2782d9be905cb972a57ff9e597fbbbefbebb3f60c0007fb3cd36f36b6a6aacba457f6b6bab3f76ecd894e1b9249f6967cf9e1d146c5c70c105296dc54cd45f48761675642cec48cab6dc849565cb96f9679d7596bfcd36db04cbe31e7bece13ff0c00329e3f534f92c173a23468c089693b973e7a6b485994ccb8de9c3cebe8c53c195916f9153ba62abdb7badf919413d3c9fa2ab7c3f4751956a7d5cb162c5daf9d3bf7fff609b7fdb6db7a58c676bf6dd77dfaefdbb363367ce4c19b714c9b43e22952cd757799e77871e8e78725df70ae9d3979a9a9a06ea36e42666fb6a003d20e7d51bca3aff7dc970c968c933b24f5c268ff3647bfa903c36cbf3231dc7f9ba9e1699653b2e3e6ffc4cfff9b73ff05b662d4a29b04a975ba6be9df21a24bf705c0c202c1c37030000004085b1f54430db1792c71d775c70d1fae73fff79508072f5d557fbe79d775eca7851e6f0c30ff78f3aeaa894e1b9249f692ba8e02a6b614752b6e526ac9875a877efdefe19679ce1df7df7ddfeb061c3fca79e7a2a65bc9e269fe54267e4c891c172624bc15592e953d3b77ab8e506f7caadd82953b155b2a82a5d7b2e4557b9be7f684ab53e260bae0e38e080605bbfdf7efb05ebda4b2fbd9432ae8d993a75aaffd0430ff9071e7860f0ef78f0c10783e71f7df451cab8a54877eb23d6705df71bb27c7fd0d0d0b0a56e43fc485ffe56faf415c77106e936e4ced6f3050025d35bb6a33bcbb6e0179ee75d2a8f7f952c907c226993edec0df2786c2291f86e7575757f3d31b21f1727ef5cb570e9f294e2aa74b970c26b29af41f20bc7c500c2c271330000000054185b4f04337d21f9f6db6f0717dc0f39e49094b6646ebdf5567f871d76f037dc70c3e042fd2bafbc120c4f162799bb637df39bdff437de7863dff3bce08e59b94c77e38d37fabbeebaab7fd8618705c34d814df2ae46dffbdef7d616019c7ffef95d0b28fc3befbc33ebeb774da669c78d1be7efb6db6ec17bedb3cf3efe33cf3cb3ce67330557ab57aff6870e1d1a8cf3c4134f647dcfeee6854e5cbe90ccb4dc84953973e604f3b1b6b636a52dd37293a94f4c8ab94c9969060d1a14dc71cbdc01ce4c675bc1558c0dee95bde8295d3155d762aba474e3652bbaeaee7dadd2d3f5315970e5384ef0dc142c99e777dd7557f03cd3f29f5cf7aeb9e61aff1bdff886bff9e69b077779bbfefaebfdadb6da2ac8983163d6be4fa6edebb7bffd6d7fbbedb60bb6ade679434383bfde7aebf90b162cc8f8dee962b6ade6f3987f4f77ef1966ca787d2c2ae9af16c9697a38e2478e712f91cc686c6cdc4cb7213fb69e2f008896d9beca3ef347b28df8953cde6db6b992959259f2fc5ed77547cae3211431673f2e36455423ef9f91329c84178e8b018485e36600000000a830b69e0866fa42f2cf7ffe7370e1fa861b6e089e777474ac8db928fee28b2f060559471e79a43f7af4687fcf3df7f4f7de7bef60dce4457873f1fdf6db6f0fee90659e3ffcf0c3394db7d1461bf9bffbddeffca79f7e3a186ea6bbe5965b828bf89b6cb2895f5d5d1d0c7ff2c927fd7efdfaf9fbefbf7f70379379f3e6657dfdae4937edabafbe1a5ce43ff4d043fd7beeb927f8592df36ff8f8e38fd77eb60b2fbcd03ffdf4d3fdbe7dfb06f3c8bc56b6f7cc362ff4673289cb179299969bb0327efcf860be996540b7a55b6eb2f58949b1962933be79ef830f3e38b8eb96290c31cf29b82aaac1bdd2173fa52ba24a576c95946efc74455799decf5a3d5d1f93055766199f3f7fbe5f5f5f1f2cf366d9cfb6fc27d7bd2db6d8c2bfe38e3b7c73672cf3fc873ffc6150ac65b67b5ffdea578371b36d5f4dc19699ceac4fcb972ff7070e1c18142f667bef74d10557d9de534f5bcc94f9fa5814724cf40b73b1d8719cbeba0df122fdd82c794dd6bfcd751bf267ebf90200fbc83e74fd4422b1b76c7f8f715df75a799c22db900f258b2413e5f9efe4d195c7dd9b9b9bcdcf6c57846cc7c5fa0e56d9a2a7258585e3620061e1b819000000002a8cad278299be907ce49147820bd7d75e7badffe9a79f762d50f0df7fff7dffca2baf5c6798499f3e7d820bddc98bf0279f7c72d72fd9fc9b6eba29a7e9860f1fbef67398022f53cc622e9a27c737774349b6f7efdf7f9d9f7fcbf6fafadfa8a735ff56337e6b6b6bf0dc14e398e793264d5afbd9cc7b9bc7ae3fad98ed3db3cd0bfd79bab45b2fd37213561e78e08160be99bb58e9b674cb4db63e29e632657e66d33c6f6b6b0bc6bde8a28b82e7145c15dde05ea94550c7f45ab75fb2155b25a52bba6aecd29eee7dacd7d3f5315970d535d75d775dd0966df94fae7ba79c724a30ae99c63c4f16949af5c8ac67e6ef6cdbd7458b16f9ebafbfbe7feaa9a7fa13264c08869bbb6c657b6ffd6f30d10557d9de534f5bcc54c0fad8233535351bca31d1bb921fea36c48bf4e10592598ee36ca1db50185bcf1700c4876c47b69363a21a791ce5baee9fe5f16dc96792e725b7c8b093e5717fd9766faca72d07d98e8b755155b6e8694961e1b8184058386e06000000800a63eb8960a62f24df78e38de0c2f4d1471f1ddcd16aca9429c1cfb99961a6e0ea8a2bae08fe3677c0326dc99871bbfefc9e792d33dc3c37c532f94c6792bce07ee69967065fd6993b08edb5d75e6bdb75714cb6d7d7ff463ded55575d154c3b75ead4e079b278c03c4f7e36f393803beeb8a3bfedb6db064502ddbda7fe37759d17faf398c4e50bc94ccb4d589939736630df9a9a9a52daf43c36c9d627c55ca692cb4cf26e6c975d7659f09c82ab500ceeb56e3154d7e2a95c8aad92324da75f3f367aba3e260bae0e3ae820ffe69b6f0efe36779d326dd9967fbdee99ed9a796edacd73f31385e66e80e6ef6cdb57f3dcac7366bb7acc31c7f85b6eb965f099b2bdb7fe3798e882abeede33ac54c8fa5830e9a74be59868ac1e8e78917efcb5f4e36c79dc4ab7a170b69e2f0088b72143866c22e7f2ff2bdbec53643b73abe485ce22ac3992f1920b5cd7fd697d7dfd57f5b47193edb8581755658b9e9614168e8b018485e36600008ac0f7fd3ed3a64dfbf99831631ebfe8a28b969e7beeb9ab4e3ef9e42fcc8ef6a4934efaf788112396c9b0b72fb9e492ab870d1bb69d9e1e008052b2f54430db17923ffde94f838bd3679f7d76f0936e3ff9c94f82e7a6e06afaf4e9c19d46cc4f488d1d3b36f8b92773b71f339dbe08dfb5c8289fe94c9217dc659f1e5c3c37774be95a1c637e1e6aa79d76f2efbfff7effe5975fcefafa3a7ada19336604c501871c7248f0f353bbedb69bbfd5565bf91f7df4d1dacf76fef9e7fbafbcf28a3f60c000ff88238e085e27db7bea7f13055785e7c0030f0cfadfdc5d6cdcb871fec5175f1cdc594acf63936c7d52cc65ca146e98d73aecb0c3826566e79d770e9e5370159ac1bd528baecc1daa722db64ad2d3e9d78d959eae8fc9822b5320659e1f7becb141e1e1f3cf3f9f75f9d7eb5eb682ab6cdb57d39ebcaba2f979c2b3ce3a2b1896edbdd345175c657bcf65cb96f993274ff6972c5992f16ffdfab9a682d6c7bcc932b1b31c0f2d916ca3db101fb2ae9d237953faf32bba0d3d63ebf90280f2637ed6d775dd6f4864d35377b9e4ef92f6ba353f4b3859728db4354abe3d7cf8f07e7a7a5b653b2ed64555d9a2a7258585e3620061e1b81900801ef07d7fc0638f3d76eba851a3fe75fcf1c7075fbc3ff7dc73fe3befbce37ff8e187be611ecd7333dcb41f77dc71ff39edb4d3e69e70c20987e9d70300a0146c3d11ccf685e4e2c58b83bb8d9802947efdfa05771d3117ae9317c8c78c19e3efbaebae6bdb2ebdf4d260b8be08af8b8c729dce64e1c285febefbee1b5cfc37452d679c71c63ac531e6ce2783060df237dd74d3a00827dbebeba49bf6de7bef0dee60b5c1061b04ef9bbc7391fe6ce6ee2fe6b929d8c9f69e7a3a3d2f74e2f28564b6e526ac983b8a99820ad367667930c513990aae4c32f549b1972953b83570e0c0e035cc67309f8582ab500dee55dce2a862bf5ec9f5747dd405574b972ef5b7df7e7b7f975d7609ceab322dff7addcb5670659269fb6ab272e54a7febadb70ea6378552c9e199de3b5d74c155b6f7343f2b68d6f15b6fbd35e3dffaf5734d85ad8f79715df76f723c34420f477cc87a76b6f4e13f289a0b87ade70b002a474343c396b2bf3ed414d7ca3669ac3cbe2e8f2b25af4aee92e767d6d6d61e24c77983f4b436c8765cac8baab2454f4b0a0bc7c500c2c271330000057aeeb9e77e79c92597ac323f01336dda34ff3ffff94f5060d51d339e19fff4d34ffff729a79cf2b29c38eea45f1b008030d97a2298ed0b4952fac4e50b49961bbb1297e5a6888a552455acd78914eba35da9c0f531279ee7d5c8b1d01b71ba4b06d6652eb24b1fce711c675bdd86e2b0f57c0140656b6a6a1a20dba77d24c7496e943c25f954f29eebba7f91c74b641f7174e7f7edf9de79b6a8b21d17eba2aa6cd1d392c2c2713180b070dc0c00409e7cdfef3d69d2a4b1279e78a23f71e244fff3cf3fd735553931d399e9e544f13339113c44bf0f000061b1f54430db1792a4f489cb17922c3776252ecb4d91f5b458aaa7d35b83f5d1ae54e8fa9855e785da776a6b6b0fd66d8807cff34e337d28d94eb7a1786c3d5f0080347a2712891d65bb7594ec232e92c78725f3e5ef65f2f8b4ebbabf97c713649c7d1dc7d9404f1c966cc7c5baa82a5bf4b4a4b0705c0c202c1c3703009007dff77b4f9830e1f9934f3ed97ff3cd37750d5541cceb1c7ffcf12be5a4ef38fd7e000084c1d613c16c5f4892d2272e5f48b2dcd895b82c372128b468aad0e9acc4fa68572a787dcc488e8146b9aefb673d1cf1e079de29d287731dc7d95eb7a1b86c3d5f00805c3535350d94fdc660d9ef9f218f77c876ed65c90ac91bf2fc4ff278ae3cfe44f6295fd1d31643bae3e20b27bc965250952d667cfd1aa4b0705c0c202c1c37030090077367ab934e3ac9efe8e8d075533d625e6fd8b061cbf95fb6008052b0f54430dd179224bac4e50b49961bbb1297e52624f9164fe53bbef5581fed4a85af8f291289c4d7e4186809c53af1e479de89d27ff3243be836149fade70b00d013555555ebd5d7d7ef29fb94a1aeeb5e2ddbba27ccb181a4433249865f298f093956d843d2574f9f8f74c7c5b74c7d3ba5a82a5bccf8fa354861e1b8184058386e06002047cf3efbec2f4db155b1ee6ca5c9eb7ed1d8d8f8b19cccedacdf1b008062b2f54430dd179224bac4e50b49961bbb1297e52644b91651e53a5eacb03eda15d6c775c9f1cf04d775ff4f0f87fd3ccf3b5efaef5dc771beaedb100e5bcf1700200cb27fd93691481c2edbbef325f74bdeaa5b7337ac1725b7cbf1c3a9f2f8c3fafafa4df5b499a43b2e5ef4e172ffaa896fa61456a58b19cf8caf5f8314168e8b018485e366000072e0fbfe804b2fbd74d5c48913759d54513df2c8231fcbc9dd63fafd010028265b4f04d37d2149a24b5cbe9064b9b12b71596e42d65d315577edb1c5fa6857581fffcb75dd43e5f8e7edeaeaeafeba0d7693be1b267df79e3ceea4db101e5bcf1700a0541a1a1a36927dcf0f647b7892e40f926992e592b9756b8ab82f94fcccdc41534f6b705c6c57382e0610168e9b0100c8c1dffffef75bce3cf34cfff3cf3fd7355245655efff8e38f5f525b5b7b90fe0c0000148bad27827c216957e2f28524cb8d5d89cb725302998aaa320d2f0bac8f7685f5718de1c387f7f33cef4d39fe3942b7c16eaeeb364abf2d482412bbe83684cbd6f3050088527373731fd937ed2a71643b79993cfe4d1e17cae3c7f2d82ab9beb350f83b8f3df658cab119892e1c1703080bc7cd000074c3f7fd3e175c70c1bfa64d9ba6eba342f1f4d34f7f223be8a7f4e70000a0586c3d11e442bd5d89cb17922c3776252ecb4d89e8e22afdbcecb03eda15d6c7353ccf3b478e7dfeaa87c36ed26f43cd456cc77176d36d089fade70b006023d9676d2edbcd1fcbe3d9f238c675dd990d0d0dfeca952b538ecf4834e1b8184058386e0600a01bcf3efbec51279c7082ff9ffffc47d74685c2bc4f7d7dfd27e6b7e3f5670100a0186c3d11e442bd5d89cb17922c3776252ecb4d09258bac46743e966db195c1fa6857581f7bf532e7d572dcb3249148eca8db602f4f48bfbd2f0fbbeb369486ade70b00101793264d4a393623d185e3620061e1b81900806edc7df7dd2da3478fd67551a1bae8a28b66b9ae7bb2fe2c00001483ad27825ca8b72b71f94292e5c6aec465b929a5dd76dbed8c01030698f952d6c55606eba35d617d0c8e79c6492ed6c3612fcff36aa5cf16398eb3876e43e9d87abe000071c171b15de1b8184058386e0600a01b975c72c99252fd9c60d2134f3cf1baeca427e8cf02004031d87a22c8179276252e5f48b2dcd895b82c37a524dbdca98ee354c47c617db42b95be3e7a9e3758d6bff9b2fe6da0db6027e9af5f98622bd775bfa5db505ab69e2f00405c705c6c572afdb8184078386e0600a01be79c73ceaab973e7ea9aa850bdf5d65bf36527fda2fe2c00001483ad27827c216957e2f28524cb8d5d89cb72532ab2bdad32dbdcce54e9f672c3fa68572a797dacaaaa5a4fd6b9d75cd7fdb96e839da4bf8e92b47b9eb7976e43e9d97abe00005133ff992297e37a8e8bed4a251f17030817c7cd000074e3a4934efae2a38f3ed23551a1faf0c30f97cb4efa7dfd59000028065b4f04f942d2aec4e50b49961bbb1297e5a6543a2fc8240baea6eaf672c3fa68572a797d745df70c59e71ed7c36127e9ab2192c58944626fdd8668d87abe000051eb7a6c9fadf08ae362bb52c9c7c500c2c571330000dd686868f0fffdef7feb9aa850c9fb2d939df46afd59000028065b4f04f942d2aec4e50b49961bbb1297e5a614cc05982e17642ae22e57ac8f76a552d747cff3b69275ed03c77176d36db08febba3f35c556f2f83fba0dd1b1f57c0100a296e6f83e6de115c7c576a5528f8b01848fe3660000ba71e28927febbd477b85ab264c9db75dce10a0010125b4f04f942d2aec4e50b49961bbb1297e5a6143a2fbea45c90d1e39513d647bb52a9eba3ac6777baae7b851e0efb483f554b7f7548f6d16d8896ade70b0010b534c7f7690baf382eb62b957a5c0c207c1c370300d08db3cf3e7bd9dcb973754d54a866cf9e3d5d76d22feacf02004031d87a22c8179276252e5f48b2dcd895b82c376133175bd25c8449a64a8f5f2e581fed4a25ae8f9ee7ed27ebd802c77136d66db04b2291384cfaaa431ef7d56d889eade70b0010b534c7f63a41e115c7c576a5128f8b019406c7cd000074e3fcf3cfffc7b469d3744d54a81e7ae8a1bfc84e7a82fe2c00001483ad278293274ffe62d5aa55295f8c91d247fa61514b4b4b2c7ede98e5c69ec469b9095be785167df165ed45183d7eb9607db42795b83e363737f791f5eb254fe836d845bae810e9ab0ed7757fa0db60075bcf1700404b73ac6d4528b8b22b145c01088bd9e6eb610000a08b8b2ebae8ead1a347eb9aa8509d7aeaa9135dd73d597f1600008ac1d613c1b6b6b6451d1d1d295f8c91d2e7dd77dfbdafa5a5e505dd473662b9b127715a6ec25497fdee56c954e9e9ca01eba33da9c4f551d6ab9324ad7a38ece279de8fa49f3a6a6b6bff57b7c11eb69e2f008056eaed559ae3fa64a6763dc6a7e0caae507005202ca5de0f0100103bc3860ddbeef8e38fff8fd07551a190f7592c3be8258ee36cab3f0b0000c560eb89e09429537ed6dadafa697b7bfb52ee90124d64beb7cf9f3f7f5c4b4bcb7b9223741fd988e526fac471b90953e7c5167d0146a72cef7215c7f571e5ca9529c3e29c4a5d1f1b1b1b3793f5aaa3bebe7e4fdd067b980bd1920f243fd46db08bade70b00a0957a7b95eeb8deecdff478145cd9150aae0084a5d4fb21000062e9b4d34e9b57aa9f15bcefbefbee951df453fa330000502c369f089a8bc3e68e1c927f9a2fc448c963e6bb99ffb1ba486f3e6fe7e78eed7263d64b3d2c4689e572534a366f778bcd2c079dcb83f5ebe3a44993fcc6c6c6e051b7c53815b93ebaae7bb3e779d7e9e1b0476d6ded81a6d84afa69b06e837d2a69bf0520de4abdbdeaaed02ac91c97e9a29f5cb262c50af3ef09d2bf7f7f7ff7dd77f76fbbedb694f174162e5ce80f1c38d0bfe28a2b52da724d6b6bab3f76ecd894e1e93262c488e033ce9d3b37a5ad6b66cf9e1d8c77c10517a4b4e9e4f3fef9c6f4c7ba3d0400c551eafd100000b174c209271c76fae9a7fffbf3cf3fd7f5514525afbf4076ce736a6b6b0fd29f01008062e144303c9d5fba327f9137969bf246ffdac9dc0d49fa66b61e8e78492412df957e5ce438ce97741bec607e3eb06ecdcf08f25d474cb0df021017a5de5e75576895d4d382ab030e3820283eda6fbffdfcdebd7bfb2fbdf452cab83a53a74ef5dbdbdb5386e79ac30f3fdc3feaa8a35286a7cbc891238b5e7095cffbe71b0aae0084a5d4fb21000062ebd4534f7d79e2c4895fe822a922fae2ca2bafbc5376ce13f57b0300504c9c0886c37ce96ae66d67aa743b900deb6579a37fede4baae744ddd783d1cb1d25bfa709aa44937c00e9ee7ed57b7a6d8ea60dd067bb1df021017b66eaf7a5a70e5384ef0fcc1071f0c9edf75d75dc1f35b6fbdd5df61871dfc0d37dc3028ca7ae5955782e1bab029d378267ffce31ffd9d76da29b883d62ebbece2bff9e69bfef9e79f1f4c9fcc9d77de99f2d9cc3883060df2f7d8630fbfa6a6669d82ab4cef97ebe7caf4fe99c6cf37145c01088badfb210000ac2327393b0f1b366cb9390109c3934f3e79b7ec98dbcdfbe8f70600a09838110c475de7ddad3a3355b703d9b05e9637fad74ed22f97489af570c487ebbac34cc195fcd95bb7217ad237df37c556d24f87ea36d88dfd1680b8b0757bd5d382ab238f3cd29f3f7fbe5f5f5f1fdce1eac5175f0c62fe366da3478ff6f7dc734f7fefbdf70ea6eb5ad8946d3cd3d6a74f1f7ff0e0c1c11db49a9b9bfd55ab56996b137ebf7efdfcfdf7dfdf7fe8a187fc79f3e6adf3b94cbb79fd830f3ed8bffbeebb832228f3dc145c657bbf5c3f57baf7cf367ebea1e00a40586cdd0f01006025cff30e39f6d86357767474e87aa91e9193a7bfca4e7911ffe31300500a9c08165fddba77b7e22e57c81beb6579a37fed24fd32c1755d470f473c989f10943e6c973efc1fdd86e849bf7cafb3d8aa5ab7c17eecb700c485addbab9e165c75cd75d75d17b45d79e595296da678ca4cd3b5b029db78c9b62953a6a4bcb7b9e355a69ff4bbfaeaab83e9dadada82e7175d7451f0dc145c657bbf5c3f57baf7ef6efc7c42c11580b0d8ba1f0200c05a8944e2b861c3862d2bd69dae3aef6cf5bee458fd5e0000848113c1e2ab5bf7ee56dce50a7963bd2c6ff4af9da45fde721c670f3d1cf120fd77bdebba37ebe1885e2291f8aef4cf62c911ba0df1c07e0b405cd8babdea69c1d541071de4df7cf3cdc1dfd75c734dd076c5155704cf6fb8e186a0602a99d5ab57af53d8946dbc64e1d4d4a95353de5b173c75cd55575d154cf7f4d34f07cf2fbbecb2e0b929b8caf67eb97eae74efdfddf8f984822b0061b1753f040080d5cc9da81a1a1a3e7ee491473efefcf3cf750d554e64ba05575e79e59de64b48ee6c050028254e048bab2efdddadb8cb15f2c27a59dee85ffb545757f7977e59397cf8f07eba0df6abafafdf53faafc3719c41ba0dd1927ef98ef99e433244b7213ed86f01880b5bb7573d2db892639ce0f9b1c71e1b14223dfffcf3fef4e9d3833b3cedb7df7ec1cf019a9fd933055466bcae854dd9c633055366bcaaaa2a7fdcb871fe79e79de7bff7de7b41dbe69b6feeefb4d34efefdf7dfefbffcf2cbeb7c2e53a065a63becb0c3fc7beeb9c7df79e79dd7165c657bbf5c3f57baf7ef6efc7c42c11580b0d8ba1f0200c07aaeebee944824261d7ffcf14be444e593fffce73fbaa62a2d19afe3befbee1b2b3be13992bf9bd7d1af0d00409838112caebaf477b7e22e57c80beb6579a37fed23e761df967e99a587231e3ccf7b5272a21e8e6875ae57edd23747ea36c48badfb2ddff7d79f3b77ee03d3a64dfbd7e4c993830be8a4b491f9fe455b5bdba22953a6fc4cf70f10055bb757667dd1453fb944175c2d5dbad4df7efbedfd5d76d9c5fff0c30ffd3163c6f8bbeebaabdfaf5f3f7fcb2db7f42fbdf4d260bcae854de679a6f14caebdf6dae0354d21d75e7bede5b7b7b707c3cd9da4060d1ae46fbae9a6413196fe6ce79e7bae3f70e0c0601af33ee6fd4cc155b6f7cbe773a57bff6ce3e713d31febf610001487adfb21000062a3b6b6f620d9a1b6d5d7d77fdadcdcfcdae4c99367bdf5d65befc9c9d067beef2fffe0830fe6befefaebd31f7ef8e1474e3bedb4bfcbb84bcdf8663afd5a000094022782c55397fdee56dce50a3963bd2c6ff4af7da44f1292fbf570d8afb3efa6cbf9771fdd86e874de75cc145b1daddb103fb6eeb7e6ce9d3bbeadadcdefe8e8f057ad5a9572419d841f33dfcdfc6f6d6dfdb4a5a5859f0d45e46cdd5e155a7055685e7ffdf5a0b069d4a851296d84822b00e1b1753f040040ec388eb3adec584f725df72ff2f8826441e74556f3f842e7f093cc787a5a00004a8913c1e2a9cb7e77ab64a6eae9008df5b2bcd1bff6913eb94cced12ed4c36137399fde58fa6ea1f4dd0f741ba2e379de37a55f1649bf38ba0df164eb7ecbdcd9ca14fbe80be9a4f4696f6f5fdad2d2f282ee23a0d46cdd5e95b2e0aab5b5d5ffd5af7e15145cdd76db6d29ed84822b00e1b1753f04004059901dedbf1cc7595f0f0700204a9c08868bf98b42b0dc9437fad73ed2270f73279ef8913ebb52faee4e3d1cd1715df71bd227efcb639d6e437cd9badf323f23c89dadec88e987969696d5ba8f8052b3757b55ca82aba38f3e3af8c9bd1ffff8c7c14f10ea7642c11580f0d8ba1f0200a02cc88e76b9f91fb87a38000051e244305ccc5f1482e5a6bcd1bff6913e99e379deee7a38ec65fa4bfaed838686862d751ba2e138ce6e756bee3856afdb106fb6eeb74a59c040ba0f050cb001db2b924bd85e01088badfb210000ca82ec683f741c67901e0e00409438110c17f3178560b9296ff4af5de41c6d03e99395555555ebe936d84bfaec71c9e97a38a29148247691fe58e0ba6ea36e43fcd9badfcab580e1b34fdafdb92fdee9bffec4c541ccdf66981e8ff42c1430c00671df5e91d284ed1580b0d8ba1f0200a02cc88eb6dd719cafe8e10000448913c170317f5108969bf246ffda45fae33baeebced4c3612ff3f38fa6cfe4fcbaaf6e43e9493fec2cebd17b9226dd86f260eb7e2b970286e51fbfefcf9a34ca9ff1b791ebc40c336d7a7c52782860800de2bcbd22a50bdb2b0061b1753f04004059901dedbb8944e26b7a38000051e244305ccc5f1482e5a6bcd1bf76f13c6fa8e44f7a38ec545353b3a1ac43f36b6b6b0fd46d28bd4422b1a3f9ae43729c6e43f9b075bf954b01c382d7ff92526c95ccc2d7ff9a323e293c1430c00671de5e91d285ed1580b0d8ba1f0200a02cc88ef61fe636fb7a38000051e244305ccc5f1482e5a6bcd1bf76f13cef77d22717e8e1b093f4d52592717a384a4ffa610753fc2619aedb505e6cdd6fe552c0f0c6d42b520aad92316d7a7c52782860800de2bcbd22a50bdb2b0061b1753f04004059901ded2ccff3bea987030010254e04c3c5fc4521586eca1bfd6b17d775ff227d72941e0efb485fed247db544b28d6e436999bb774b3fcc933e3959b7a1fcd8badfcaa58061564b734aa15532a64d8f4f0a0f050cb0419cb757a474617b05202cb6ee870000280bb2a37d59f21d3d1c0080287122182ee62f0ac17253dee85fbb487fbce3baeeae7a38ec237df5a8f4d5483d1ca525fdb05de77a73aa6e4379b275bf954b01030557a50b050cb0419cb757a474617b05202cb6ee870000280bb2a37d4ef27d3d1c0080287122182ee62f0ac17253dee85f7bd4d4d46c28fdb1c2719cbeba0d76715df7a7d2576f0c1f3ebc9f6e43e9d4d7d77f55fae16de98f33741bca97adfbad5c0a185e7fe2e29442ab644c9b1e9f141e0a186083386faf48e9c2f60a40586cdd0f010050163ccf7bb2b6b6f6403d1c0080287122182ee62f0ac17253dee85f7b241289ef4a7fbcaa87c32ed5d5d5fd4d918f9c531fa2db503ad207db48fee1baee59ba0de5cdd6fd562e050c739eb929a5d02a19d3a6c72785870206d820cedb2b52bab0bd0210165bf74300009405cff35a6a6b6b0fd6c3010088122782e162fea2102c37e58dfeb587ebba8d729e76af1e0ebbc83a7381e4413d1ca53374e8d0ada50fdee2271d2b93adfbad5c0a183e7c7fa63febf1dfa4145b9961a64d8f4f0a0f050cb0419cb757a474617b05202cb6ee870000280baeebfe2d91481cae87030010254e04c3c5fc4521586eca1bfd6b0f3947bbc2f3bc5febe1b087e338dbcb3ab344cea5bfa6db501ab28e6c257df086ac2fe7e93654065bf75bb91630cc9b3e26a5e0ca0cd3e3919e850206d820eedb2b529ab0bd0210165bf74300009405d7751ff23cef483d1c0080287122182ee62f0ac17253dee85f7b485f3c2a19a287c31e721efd67e9a30bf47094464343c3969ee7bd2efdf07fba0d95c3d6fd564e050cab57fb739efd434ac1951966da52c62705870206d820d6db2b52b2b0bd0210165bf74300009405d9d1de2771f5700000a2c48960b898bf2804cb4d79a37fed217d31cf75dd9df470d8c1f3bc43a48fdeaeaeaeeeafdb103ec771b690f9ff9ae437ba0d95c5d6fd5677050ccb3f7edfffc73337a6145b2563dacc387a3a52582860800de2babd22a50ddb2b0061b1753f04004059901ded1849831e0e00409438110c17f3178560b9296ff4af1d1cc7d958fa62457373731fdd86e80d1f3ebc5fdd9a9fb1fba96e43f81a1b1b3793f93f4372b16e43e5b175bf95b18061f56abf7dce14ffb5c7ce4f29b2d231e39871b9db55cf4301036c60ebf66af2e4c95fac5ab52a65bd21a58ff4c322d95eadd67d0400c560eb7e080080b2203bdadb25c7e9e10000448913c170317f5108969bf246ffdac175ddef495fbca487c30ed23f23a57f1ed5c3113ec77106c9fc7f45e6ff65ba0d95c9d6fd56ba82abeeee6a9529dcedaae7a1e00a36b0757bd5d6d6b6a8a3a32365bd21a5cfbbefbe7b9f6caf5ed07d0400c560eb7e080080b2203bdad1aeeb9eac87030010254e04c3c5fc4521586eca1bfd6b07e98726c9183d1cd1937ed946b2849f7b2cbd4422f165538828b95cb7a172d9badf4a577095cb5dad32c54cab5f8fe41e0aaeacb3a11e50096cdd5e4d9932e567adadad9fb6b7b72fe54e57d144e67bfbfcf9f3c7c9b6ea3dc911ba8f00a0186cdd0f010050165cd7bdd6f3bc33f5700000a2c48960b898bf2804cb4d79a37fed20fd7095e45c3d1cd1937e192bb9440f47b89a9a9a06ca7c7fd1ac1bba0d95cdd6fd56ba822b5d44956ff4eb91dc43c19555064b3a3a1f2b8aaddb2bc314f9983b2b49fe69d61752f298f96ee63fc556004263f37e080080d8735df70acff3ced1c3010088122782e162fea2102c37e58dfeb583f4c3443947fba91e8e68d5d6d61e287d33bfa6a6a622efcc1195fafafa4d65be3f2fb946b701b6eeb7cc05745df443a28be90fdd478844b2d86a44e76345155dd9babd02005406f6430000844876b497482ed0c3010088122782e162fea2102c37e58dfeb583f4c3bb8ee37c5d0f4774a43ffabaae3bd3f3bca3751bc23364c8904d647d9826b95eb70186adfb2d0aaeec0a05575648165b258bacf4f3b267ebf60a005019d80f01001022d9d18e925cac87030010254e04c3c5fc4521586eca1bfd1bbdce0293cfe4cfdeba0dd1913e39ddf3bc163d1ce1711c676399efcf486ed26d4092adfb2d0aaeec0a055791cb545c95697859b2757b0500a80cec8700000891f93941f3b3827a38000051e244305ccc5f1482e5a6bcd1bfd1933ef8bee4453d1cd1696868d852fae403396fde5db7211c32cf379279fe94e40fbd283e4416b6eeb728b8b22b145c45aabba2aaeedacb86addb2b004065603f040040883ccf3bd375dd6bf5700000a2c48960b898bf2804cb4d79a37fa3277d70ace42e3d1cd191f3e53b2457eae108474d4dcd86b20eb44a6eed45b115ba51eafd96bcdf5449951eae5170655728b88a4caec554b98e176ba5de5e0100d015fb21000042e479de29aeebfe5e0f0700204a9c08868bf98b42b0dc9437fa377a726ef6ffe4dc6ca41e8e68485ffc40d68b85e6e7ed741b8a4fe6f306b20e4c9179fec75e145b2107a5de6f99f7eb4cd6c22b0aaeec0a055791c8b7882adff163a7d4db2b0000ba623f040040883ccf3bde75dddbf4700000a2c48960b898bf2804cb4d79a37fa327e7658f251289c3f570945e7373731f5927a64b12ba0dc5678aad645e3f21b9cbcc7bdd0ea453eafd569782abac8557145cd9150aae4aaed0e2a942a78b85526faf0000e88afd10000021921d6d83648c1e0e00409438110c17f3178560b9296ff46ff4a40f16241289afe9e1283dcff34e943ca987a3f89a9a9a06c8b23f49720fc556c847a9f75b5d0aad74d629bca2e0caae507055523d2d9aeae9f4d62af5f60a0080aed80f01001022d9d1ba92fbf4700000a2c48960b898bf2804cb4d79a37fa3e538ce973ccf5bd68b9f528b9cf4c520591f164b7feca5db505cd5d5d5fd655eff5d3256e67b5fdd0e6453eafd569702ab4c090aaf28b8b22b145c954cb18aa58af53a5629f5f60a0080aed80f01001022d9d11e2599a087030010254e04c3c5fc4521d25c58246516dde7281d99fffb4b9ed7c3517ad20f7f705df7063d1cc5e538cefa32af1f95dc47b1150a91dc6fe97d59d4a1e0caaee8fec9257a5943b78a5d2455ecd78b1ccb1500204aec8700000891ec688f903caa87030010254e04c3c5fc0500bbc876f904cff3eed0c3515aaeebfe8ff4457b5353d340dd86e2193e7c783f99d77f91793dbeaaaa6a3ddd0ee422793c5baae35a5d94d32553ebf849416b93ef1dae4ab53c95115d1c65eed4794ce7633ef474fa75638de50a001025f643000084a8b6b6f660cff35accdf9d3f9db0833cdf4a8f070040297122182ee62f00d845cec1ae939cad87a3a47acbfef159d77587e906144f67b1d54332af1fa4d80a3d913c9e2dd5716d77855649145cd9150aae42a58ba24cb1d41f25661e9ac75c8bae324da75f3fb658ae000051623f04004091d4d7d76f2a3bd6d724f3242bd27c59144373df6f0000800049444154c4f3bc73f4b4000094122782e162fe02805dcc7f8291fc440f47e9c8beb14932ad57ee17889127536025f3f841c9c3a6f04ab703f9481ecf96eab8b6cbf766690bad9232155cad58b1c27cce207dfbf6f5b7df7e7bff8c33cef0972e5d9a32aeceecd9b383e92eb8e08294369dd6d6567fecd8b129c3cb35ddcd1b0aae4293ae18ea985e9dcb78677229baea5a6c954c6397f674ef133b2c57008028b11f0200a08864c77a49972f8952e2baeec7a6304b4f070040297122182ee62f00d845b6cb8b24dbe9e1280dc771be64fa2091487c57b7a1384cb195e7790f989f1294f9bdbe6e07f2953c9e2dd5716d5d37855649dd155c1d70c001fef8f1e3fd912347faebafbfbebffffefbfbab56ad4a19bf6bba2b2aea9ac30f3fdc3feaa8a35286976bba9b37145c8522531154bae2a96c4557e9c6bfbd73785799de2f3658ae000051623f0400401175dee56a69d7222b95dfea69000028354e04c3c5fc05007b2412892fcb76f9533d1ca5637ed251fae0163d1cc5e1384e5f99bff7b9aefb378aad502cc9e359db8e6bbb2bb8927560edb073cf3d37183671e2c4e0f9adb7deeaefb0c30efe861b6e181466bdf2ca2b698b8a328d77fef9e7772d5cf1efbcf3ceace3eb983b6e6db3cd36fe800103fcef7def7bfe4b2fbd94f2fe3bedb493ffe52f7fd9ffcd6f7e9373db8d37dee8efbaebaefe61871d160c1f376e9cbfdb6ebb05efb3cf3efbf8cf3cf34c30fcdbdffeb6bfdd76dbf9ab57af0e9e373434f8ebadb79ebf60c1829c3e9bfef7985070955e5d8e0584690cee95bdf8295d1155baa2ab74e3a52bb64aeaee7dad5629cb1500c04eec8700002832cff3ce564556c9ac6c6868d8528f0f0040a97122182ee62f00d843cecf0ea85bf353768880ebbadf92f9dfd1d8d8b8996e43cf353737f791f93b56f2f7eaeaeafeba1d2854f278d6b6e3da7c0aae1e7df4d160d815575ce1bff8e28b7eefdebdfd238f3cd21f3d7ab4bfe79e7bfa7befbd774a5151b6f19e7cf249bf5fbf7ec15db31e7ae8217fdebc7959c7d779f8e187fd5b6eb9c5bffefaebfd4d36d9c49775769df737c55077df7db7ffe31fff3878fedc73cfe5d4b6d1461bf9bffbddeffca79f7eda7ff5d5578322aa430f3dd4bfe79e7bfcdd77dfdddf7cf3cdfd8f3ffed8bfe69a6b82f1cdbf63f9f2e5fec08103fd9a9a9a9c3e1b0557f9e9f25d703e8557837be556f494ae98aa6bd155baf66cc55649b9bebf752a65b90200d889fd10000045d6d4d4344076b0ef7539b90ee2baeeeff5b80000448113c170317f01c01e9ee79d28db6573a111119079df2a39490f47cf75165b8d913c6ebe87d0ed404f248f676d3baecda7e0ea91471e09865d7ef9e5fe95575ed9b5f824489f3e7d82e9ba1615651bcfbc66fffefdd7f949c1eec64fa6a3a3c33ff8e0838362a8e478e68e53a62df9fe679d7556f07cd2a449c17373e7aa5cda860f1fbef67daebdf6da60586b6b6bf0dc145099e766ba458b16053fb378eaa9a7fa13264c08863ff8e083397d360aaef2a3bf13aeebbef06a70affc8a9dd2155599e77dd20ccfa5d82a29dfcf61854a59ae000076623f0400400864077b9c3ab1feb7e3385fd7e3010010054e04c3c5fc05007bb8ae7b836c977fa587237c9e9079ff92290cd26de899ce62ab3b254f388eb3816e077a2a793c6bdb716d3e0557a79f7e7a30cc141b99bb5c99bf6fb8e1067fca94296b637e5eaf6b5151b6f1cc6bea82abeec64f26599875e6996706454ae62708f7da6bafa02df9fee667fdccf3c71e7b2c787ed34d37e5d4d6b518eaaaabae0a864d9d3a35787edd75d7adf3dc7cf66db7ddd63fe69863fc2db7dc32986fb97c360aaef2a3be13eeaef06a70afc28a9cd2155dbda59ee7536c9554e8e7894ca52c5700003bb11f020020048ee3f4959dec1bc9136acff3eed5e3000010154e04c3c5fc05007bc83679b2ebba87eae108979c136f2cf37e819c0befa7dbd063bd65dede2ef3764a4d4dcd86ba112886e4f1ac6dc7b5dd155c1d70c001fe9ffef4a7e02e4e7dfbf6f50f3cf0c0a0f869faf4e9c19da7f6db6f3f7fecd8b1c1cfff5d7df5d5294545d9c633313fcfb7d34e3bf9f7df7fbffff2cb2f773b7e32c9c2ac73cf3d37288a327793d2454ddb6cb38d7fd75d77f93ffad18f829f2934af9d4b5bd762a819336604ffee430e3924f849c1dd76dbcddf6aabadfc8f3efa28684fdef56b8b2db6587bd7ac5c3e1b0557f9497e1f9c25c9c2abc1bd7a56dc94aee82a99428aad927afab94aaa52962b00809dd80f01001012cff38e4e9e48d7d7d7efa9db0100880a2782e162fe02803d649bbcd8719c6df57084cb75dd2b64dedfa587a3c74cb1d5ad9ee73dd9d0d0b0916e048a25793c6bdb716d77055726fdfaf5f377dc71477fe4c8916b0b8d4cc68c19e3efbaebae41bbb9bbd3a5975e9ab6a828d37826e64e5683060df237dd74537fdcb871dd8e9fccc2850bfd7df7dd37b843d661871d16dcb14a17350d19322428e6da6cb3cdfcdffef6b739b7e962a87befbd37f8f76fb0c106c17b3efdf4d36bdb56ae5ce96fbdf5d6c174a6382bd7cfa6df2399420aaec89a98bbb10d1830c0ccbf117a3ee5c9dc4552dfd9ca3cefe9dd2547e8cf6c73f4870700a054d80f0100109ede9ee74d979dedbf7503000051e244305ccc5f00b0436363e366aeeb7eac87235c8ee3ec26fbc20fe47c782bdd861e31c5567f903c65ee20a61b81624a1ecfda765c9ba9e02ace4916358d1a352aaf361b926fc155a5d0c5405d32b56edd9f141cdcab677792ca76872b339c3b5c01001032f643000084a8b6b6f630cff3fea587030010254e04c3c5fc05003bc8f9d881b24d7e460f47b8649e4f725df70c3d1c3d23f3f526b33c0f19326413dd06145bf278d6b6e3da722eb84a7717a96c6d368482abf47228b4eaaad0e2a674c556fa4e5785145d15fa792263db760a005059d80f0100ace6fbfefa73e7ce7d60dab469ff9a3c797270224f4a1b99ef5fb4b5b52d9a3265cacf74ff0000e28913c170317f01c00eaeeb9e2cdbe45bf5708447e6f9cf659ebf565555b59e6e43e1649e5e2f99565f5fbfa96e03c240c155e992ada82a5b9b0d31fda1fb08eb145c652bb4ea2adf22a774c556b7f75af333827a783e4557f97e0e2bd8b69d02005416f6430000abcd9d3b777c5b5b9bdfd1d1e1af5ab52ae5c49e841f33dfcdfc6f6d6dfdb4a5a5e508dd470080f8e144305ccc5f00b0836c8f6fe24e4ba5e338ce0632cfe77b9e17ab0bb5b693797a8de40599bf5fd26d405828b822b98482abf4f228b4ea2ad762a74cc556c9a2aa74edb9145de5fafed6b16d3b0500a82cec870000563377b632c53efa849e943eededed4b5b5a5a5ed07d0400881f4e04c3c5fc05003b980b9eb5b5b507ebe10887ccef8b3dcffb931e8ec2c93cbd4a32bda9a969a06e03c244c115c925145c155d77454fe98aa9ba165b25a51b2f5bd15577ef6b35dbb6530080cac27e08006035f33382dcd9ca8e987e68696959adfb0800103f9c08868bf90b007690edf10743870edd5a0f47f12512891d657e2f711c675bdd86c2c8fcbc5cf2b2ccdb2feb36206c145c915c42c1552832153fa52ba24a576c95946efc74455799de2f366cdb4e01002a0bfb210080d5f822c5aef0450a0094074e04c3c5fc0580e8398eb3856c8f3fd4c3110ed775ffe279de397a380a23f3f252597e5f95e578906e034a81822b924bf89e3034e98aa08ee9957bb15552baa2abc62eede9de27766cdb4e01002a0bfb210080d5f2f922a5fda3cffcb3fef46a10f3b76e273d0f5fa4004079e044305ccc5f00889e6c8bab244fe9e1283e99cf47789ef7a6e338ebeb36e44fe6e545aeebce6c6c6cdc4cb701a542c115c9257c4f182a5d0cd5b5782a9762aba44cd3e9d78f2ddbb6530080cac27e080060b56c5fa4ac5efd4fffc3652bd73e6f7b63f1da822bf37772f8079facf03f59fedff148e1e18b1400280f9c08868bf90b00d1936df12f5dd7bd590f4771555757f797793d47e6f5a1ba0df993f978a1cccf59e60e6dba0d28250aae482ee17bc2d0e9a228532c65ee50956bb155929e4ebf6eacd9b69d02005416f6430000ab65fa22c5145bddfbcc3cffd7e367fa8fbfb6c86f9dddee8f9a306b6dc195f9db0cffdb2b0bfdf3649c6b1e7bcbfff4338aae7a1abe480180f2c08960b898bf00103dd9168ff63cef343d1cc5e5baeeffc9bc9ea087237f665eca32fb7a4343c396ba0d28350aae482ee17bc29228767154b15f2f72b66da700009585fd1000c06a99be4859fae90affdc0766ae2db0ea2e23ee9be12f58ba2ce575487ee18b1400280f9c08868bf90b00d1f33cef49c98ff470148fe338dbcb3e6f492291f89a6e437e5cd73d4fe6e51bb2cc6ea5db80285070457209df13964cb18aa48af53a56b16d3b0500a82cec87000056cbf645cac457df4f29acca94079e7f37657a927ff8220500ca032782e162fe0240f4645bbc94e29570c93c1e2f19a587233f320f4748de1a3a74e8d6ba0d880a05572497f03d6149f5b458aaa7d35bcbb6ed1400a0b2b01f0200582ddb1729e6670493055523ef9fe18f7b76beffcee24f8298bfcdb064fbdf67bc9f323dc93f7c910200e58113c170317f01205aa6d04ab6c54bf470144f6d6dedc1328fdf696a6a1aa0db903b9987bf92fc43b28d6e03a244c115c9257c4f587285164d153a5d2cd8b69d02005416f6430000aba5fb22a5fda3cffcb63716fba326cc5a5b50650aacf4786658b2dd8c6ba631d3eaf148eee18b1400280f9c08868bf90b00d1323f2528dbe2563d1cc5317cf8f07e327f67cb7caed16dc89dccc3d3256fd7d7d77f55b70151a3e08ae412be278c44bec553f98e1f3bb66da700009585fd1000c06ae9be48d13f176862ee6aa5c733c3f478267a3c927bf8220500ca032782e162fe0240b43ccf3bcd75dddfebe1280ed9cf8d90f9fb373d1cb99379f84bc93b92ed741b60030aae482ee17bc2c8e45a4495eb78b166db760a005059d80f0100ac96ee8b145d4045c155e9c2172900501e38110c17f31700a2e5baeecd9ee79da287a3e7860e1dbab5ece796388eb3b36e436e64fe9d2499974824bea6db005b5070457209df1346aabb62aaeedacb866ddb29004065613f0400b05aba2f52f849c1e8c2172900501e38110c17f31700a225dbe1a73ccf2bfb0b8c5190f97aafccdfcbf470e446e6dd70c97cc771beaedb009bd85a703579f2e42f56ad5a95f27d15297da41f16b5b4b4acd67d8492ca545495697859b26d3b0500a82cec870000564b5770954cebecc56b0baa46de3f2328b03277b532317f9b61c97633ae9e9ee41f0aae00a03c7022182ee62f00444bb6c31f799eb7b91e8e9e91f9fa43c9bb3535351bea36744fe6dd7166fe2512891d751b601b5b0baedadada16757474a47c5f454a9f77df7df7be96969617741fa1e47471957e5ef66cdb4e01002a0bfb210080d5b2155c3d36f3fd949f0bcc94475f5998323dc93f145c014079e044305ccc5f00884ee74fde990b8d2822c771faca7c9d21f9856e43f764be3549dee3a7181117b6165c4d9932e567adadad9fb6b7b72fe54e57d144e67bfbfcf9f3c7b5b4b4bc273942f71122912cb21ad1f95831c556866ddb29004065613f0400b05aa682aba59faef07f3d7e664a6155a69cf3c00cff834f56a4bc0ec92f145c014079e044305ccc5f00884e6d6dedc19ee74dd1c3d133324f4f93fddb137a38ba27f3ad41b2c075dd5d751b602b5b0bae0c53e463eeac24f9a7f99e8a943c66be9bf94fb1955d92455715556c65d8b89d0200540ef6430000ab9913795df463b262e56aff0f53e6043f1bf8d0f4f7fca9b317fba326cc5a5b6065fe6e9dddee3ffedaa2a030eb8eb677fc55ab57a7bc0ec92fa63f741f0100e28713c170317f01203aaeeb9e21dbe11bf57014ce719c2d649e7e20f3f61bba0dd9c97c4b48167a9eb7bb6e036c6673c115808c2af2277fd94e0100a2c47e080060b54c055726a6e86af1c79fad7ddef6c6e2b50557e6efe4f08f96ada4d8aa48a1e00a00ca032782e162fe024074641b7cabe4243d1c8593f9f947d775afd6c3919dcc33997575ef53a88638a2e00a405cb09d02004489fd1000c06ad90aae74da3ffa6c6dc195f95bb7939e87822b00280f9c08868bf90b00d1916df033921feae1288ccccbef4b160e19326413dd86cc5cd77564be2df23cef9bba0d88030aae00c405db29004094d80f0100ac964fc115093f145c014079e044305ccc5f00888e6c833f711c67901e8efc353737f791f9f9a2ebbaf5ba0d99c9fcfab9ccb7f6fafafa3d751b1017145c01880bb653008028b11f0200588d822bbb42c1150094074e04c3c5fc058068388eb3ad2974d1c351189997c3256d7a3832f33cef48b30cbaaefb6ddd06c409055700e282ed1400204aec87000056a3e0caae50700500e58113c170317f01201aaeeb1e2adbe027f470e4cfdc254ce6e562cff3f6d26d484fe6558d996792efe836206e28b80210176ca7000051623f0400b01a05577685822b00280f9c08868bf90b00d190edefaf24d7ebe1c89fccc7d1921bf570a427f3ea08536c954824beabdb8038a2e00a405cb09d02004489fd1000c06a145cd9150aae00a03c7022182ee62f004443b6bfb74b86ebe1c88fccc3ef48da9b9a9a06ea36a4f23cef2732bf3a5cd7fd9e6e03e28a822b0071c1760a00502ab2cf996af63bdd64aa9e0e0080c85070655728b80280f2c01792e162fe02403464fbfb5c6d6dedffeae1c84b6f998fcf488ed50d48d5f933961d92efeb3620ce92c7b31cd702b01ddb290040a9c83ea72a4d81954e959e0e0080c85070655728b80280f2c01792e162fe02403464fbfb297765ea19cff38e91f9f89cfcd95bb7615d329f7e5cb7a6d86a7fdd06c45df27896e35a00b6633b050028a5baec77b9e2ee560000bb5070655728b80280f2c01792e162fe0240e9398eb3bd6c7fdfd7c391bbfafafa4d651e2e92eca3dbb0aedadada83643e75789e77806e03ca41f27896e35a00b6633b050028a5baec77b9aad2e3030010290aaeec0a055700501ef842325ccc5f00283dd775ab65fbfbb81e8edcc93cbc56e6e1ad7a38d6e579de60994f1fd4d6d61ea8db807241c11580b8603b050028b5baf477b9e2ee560000fb5070655728b80280f2c01792e162fe0240e9c9b677842918d2c3911b9977dfaa5b73c7a6cd751bfecbdcd1ca145bd5f13f9751e628b80210176ca70000a566ce07d3145c55e9f10000881c05577685822b00280f7c21192ee62f00949e6c7beff43cef783d1cb991f937d575dd93f570fc97cca3fd251d921feb36a0dc507005202ed84e0100a260cea1bb145b71772b00809d28b8b22b145c014079e00bc970317f01a0f464dbfb82e779fbe9e1e89eebba32fbea5e6e6e6eeea3dbb086cca31f98622b59c60ed16d4039a2e00a405cb09d020044a16eddbb5c55e9760000ac6043c1d5b265cbfcb3ce3acbdf669b6dfc010306f87becb187ffc0030fa48cd7d3b4b6b6fa63c78e4d196e5328b80280f2c01792e162fe0240c9f5966deff2fafafa4d7503b2731c676399770b24fbeb36ac914824f635c556f278986e03ca15055700e282ed1400202a759d77b9d2c30100b0860d05576667d9bb776fff8c33cef0efbefb6e7fd8b061fe534f3d95325e4f73f8e187fb471d7554ca709b42c1150094074e04c3c5fc0580d292edee0ea668480f47f764be5dee79dedd7a38d690f9b34f67b1d5e1ba0d2867145c01880bb6530080a8d475dee54a0f0700c01a51175ccd9933c7ec28fddadada94b6d9b367076d37de78a3bfebaebbfaff9fbd3b0193a32cf7bf4f80b0844540082a824814706151831e1075f4a02160221cada9a9eae9388ac60d092a0822ea0828ab288851164541b6a02c8a2c86841040942d04114424095b3620211e9678fcbfd6fb7b7abac6ce3d7b4fd7d6fdfd5cd77dd5ccf354775557d7b34cf73d5593264daa949f77de79d1ce3bef1c8d1b372eda7ffffda3050b16f43ec6256dc557cada679f7da27befbdb7527edc71c7559e2b8e0b2fbc70c8e7ca2248b80280e6c01f82c9e2f80240bad4ef1eecfbfe8db61c83d331db55c7ee992008b6b775a81c9fb7ebf82cd7f2c3b60e6876245c01280afa290028a6288a365ab468d1ac3befbcf3ffe6cc9953f9fe91483774dcff3d7ffefca573e7cefd887d7f00004dc275f836e927cdb8f2ca2b2b0950679d75569fba38e16ab3cd368b4e3ef9e4e8f6db6f8feebefbeecad5b00e39e49068e6cc99d11e7bec11edbdf7debd8fb9e69a6ba273cf3db7f27c5b6cb1453479f2e44af9adb7de1a8d1d3b36da6fbffda2abafbe3a5abc78f190cf9545b8f7c3be470080e2e103c964717c01205d41107c55f13d5b8ec1b924351db7236d392ab711dcbb9a6cc507cf6849245c01280afa290028a6458b165d397ffefc68c58a15d1cb2fbfdce7fb4822f970c7dd1dff79f3e6ad993d7bf6c1f63d02003481ac13ae66cd9a5549aa7257b1b27571c2d5f4e9d37bcb4e3bedb44a596dacbffefad18b2fbe5819b40e38e08068c30d37ecaddb6bafbd7a1fbbf1c61baf734bc1c19ecbee4b5a41c2150034073e904c16c71700d2e56e89a7bef793b61c03d3f13a54f1605b5bdb86b6aed5e97cda53c766993b46b60e6815245c01280afa2900282677652bf7bda9fd1e92483f962d5bf6ececd9b3efb2ef1100a009649d70f5c0030f54129dbababafad4c50957c71f7f7c6fd9a9a79e5a293bfbecb3a3b973e7f6c6dab56b7b13a88e3cf2c84ae292bb55e09e7beed9fb589b7035d873d97d492b48b80280e6c00792c9e2f80240bad4efdea378972d47ff3ccfdb54c76bb1a2cdd6b53adff7dfaae3b254f1315b07b41212ae001405fd14001493bb8d2057b6ca47b8f761f6ecd96bed7b0400680259275cb978ef7bdf5bb92ad5b1c71e1b5d7ae9a5d109279c10b9cb5cf6977075cf3df754ae42b5efbefb46975c7249e55680679c7146a52e4ea03ae69863a2d34f3fbdf29cb50957db6ebb6d3461c284e88a2bae88eebbefbe419f2bab20e10a009a031f48268be30b00a91aa37ef785a953a76e612bd0bf2008bead6376b92d6f759ee7bdb99a6ce5db3aa0d5907005a028e8a700a098f2f0fd2ff19fe0fb5f006852791870972e5d1a0541106db3cd3695ab50edb6db6e03265cb9b8e8a28ba25d77dd351a3b766c347efcf8e8a4934eaa943ff5d453d13bdff9ceca734c9a34299a3163c63a0957ee4a566e1b5b6eb96525b16bb0e7ca2a187001a039f08164b238be00909e300c7751bffbb82d47ffaac7eb19cff376b075ad4c7ff3efaee3f2b49681ad035a110957008a827e0a008a6924dfff2e5bf542f4e5cbeeaf84fbd9d613a30fbeff05802635920197483e187001a039f08164b238be00909e2008a6a8dfbdde96a37f3a56d72a8eb1e5adccf7fd5d754c9ed2b9d469eb805645c21580a2a09f0280621aecfbdfb56bff193df78f977a7f9ffff0f2de842bf7735cbef2f917a3e7fff73feb11f507dfff0240931a6cc025d20f065c00680e7c20992c8e2f00a4c7f7fd63d5ef9e6ecbd157188607e9583de279de46b6ae55e998bc51c7e4c920083e6eeb805646c21580a2a09f0280621ae8fb5f976cf5cb3b16475fbbf281e8f77f5e1acd7b6859f48dab1eec4db8723fbbf2df2d782a3a56eb9c79e323d19a1748ba1a6df0fd2f0034a981065c229b60c00580e6c00792c9e2f802407ad4e75eace8b2e558d7e4c99337d6717a340cc349b6ae55f9be3f41c7e409c5276d1dd0ea48b8025014f45300504c037dfffbec9a17a363663dd09b6035541c75f9c2e8c967ffd1e779889105dfff0240931a68c025b209065c00680e7c20992c8e2f00a4477dee7dbeefef63cbb12e1da7e3749caeb6e5adcaf3bcd7eb983caef8b4ad0340c21580e2a09f0280621aecfbdfebef7fba4f62d54031eb4f8ff7793c31f2e0fb5f006852830db844fac1800b00cd810f2493c5f1058074747777afaf3ef7c572b9bc99adc37fe818eda87846b1b3ad6b45ee3828160741f0195b07a0070957008a827e0a008a69b0ef7fdd6d04e384aaa3af58185dfa8725d163cb9faf84fbd995c5f5372c7cbacfe3899107dfff0240931a6cc025d20f065c00680e7c20992c8e2f00a4c3f3bc37b8c4195b8e750541304bc7e95bb6bc15e99cd949c762918ec9e76d1d80ff20e10a4051d04f014031f5f7fdefb2552f44f31f5e1e7de3aa077b13aa5c82955dcf95c5f56e5df718f758bb1e31fce0fb5f006852fd0db84476c1800b00cd810f2493c5f1058074f8beff11f5b9d7d972fc878ecf7fbb04a3aeaeae4d6c5dab29954aafd5b1f87b10045fb47500d645c21580a2a09f028062eaeffb5f7bbb4017eeaa56763d5766d77361d723861f7cff0b004daabf0197c82e187001a039f08164b238be00900ef5b7c7f9be7faa2d478fe9d3a78fd5317a4831d5d6b51acff376d07178340882236d1d80be48b8025014f45300504cfd7dff6b13a848b84a2ff8fe17009a547f032e915d30e0024073e003c964717c01201dea6f2ff17d7f9a2d478f2008bea26374bd2d6f359d9d9dafd67178447194ad03d03f12ae001405fd140014537fdfff724bc1ec82ef7f01a049f537e012d905032e0034073e904c16c71700d2a1fef67edff7df6ecbd19b64f44c18866fb475adc4f3bc570541f0571d8b636c1d8081917005a028e8a700a09806fbfe77de43cb7b13aa8ebe626125c1ca5dd5ca85fbd995c5f56e5dfb7862e451e4ef7fab57b4feaceffbbfd1f22ec5936e7e505dde552dffac5bcf3e16009ade60032e917e1479c00500fc071f48268be30b00c9f33c6f03f5b72f4d9932659cad43652cba58f15d5bde4a8220d8bea3e7968ac7d93a00838be7b3cc6b01e41dfd140014d360dfffdef8c0d37d6e1738505cb7e0a93e8f27461e45fcfeb7bdbdfdfd9a07ccaf26570d37e6bbc7d9e70280a635d8804ba41f451c7001007df18164b238be00903cdff777557ffb775b8e4aa2d1fe3a368f97cbe5cd6c5dabf03c6f3b1d830715dfb075008616cf6799d702c83bfa290028a681beff7d76cd8bd1d7ae7ca04f62d540f1d5590ba395cfbfd8e77988914591befff57d7f82c6ff1bfa49a61a49dce09ec73e3700349d81065c229b28d2800b001898fba3c296a17138be00903cf5b5872aaeb5e5adae7ae52f77ab45cfd6b58a2008b6d5eb7f40c7a1dbd601189e783ecbbc1640ded14f0140310df4fdef8b2fad8d7e3cf7d1ca6d03afbee789e896879647dfb8eac1de042bf7f3bc879645bffff3d24a62d6cfe63f16bdbc766d9fe721461645f9feb7bdbdfd00fdcdff7c6df254b95c8e4e3ef9e468ce9c39d1638f3d163df7dc7391e396ee7757eeeadd7a26e96a957b3ebb0d00682a030db84436519401170030383e904c16c7170092a7bef6f88e16bf655e7f7cdf3f5cc7658e2d6f15d3a64d7ba55efffd8a136d1d80e18be7b3cc6b01e41dfd140014d360dfffbaa4abe5ab5fe8fd7dfec3cb7b13aedccf71f9aa7fbc44b25583a208dfff6accff641004ff2f4e982a954ad1f9e79f1fad5ebdba92603514b79e5bdf3dae26e9ea5fee79edb600a0690c36e012e94711065c00c0d0f84032591c5f00485e100497f9be5fb2e5adac7a1bbd955abed9d6b582300cb7d6ebbf4fe7c6c9b60ec0c8907005a028e8a700a09846f2fdefb2552ff4265cb99f6d3d31fac8fbf7bfd52b5bf5265b1d7ef8e1d1e2c58b6d4ed5b0b8c7b9c7d7265d71a52b004d6b24032e917ce47dc005000c0f1f48268be30b00c973b78c0bc3706f5bdeca34fe5c1004c1f76c792be8eaeada4aafff1ebdfed36c1d809123e10a4051d04f014031f1fd6fbe22cfdfff7a9ef786300c57c709525ffffad7a3356bd6d83caa11718f77cf539374f59cdb8edd360014de9c3973fefdf2cb2ff7e9f889f443efc3520db86bed7b0400281e3e904c16c7170092d5d6d6b6a1fada97bababa36b175ad2a0cc377ea983c3d75ead42d6c5db3f33cef157aed77b56ab219900412ae001405fd140014130957f98a3c275c69acbf3e4e8c7257a61a6db255cc3d8fb9d2d5f576db005078f3e7cf5fba62c58a3e1d3f917e3cfef8e3976bc0bdcbbe470080e2e103c964717c0120594110ecaebef66fb6bc55757777af5f4d38eab475cdae542a6da9d7fe47bdf61fd83a00f523e10a4051d04f014031917095afc86bc2557b7bfbfbe38428fdfd5ff76d0407e29ecf3d6fbc0db73dbb0f00506873e7cefdc8bc79f3d62c5bb6ec59ae74954de8b82f5bb264c9a51a6c9f501c6cdf230040f1f08164b238be0090ac20083eeafbfed5b6bc5569dcf9b4e2365bdeecdcd5bcf4baffa0f8a1ad03303a245c01280afa2900282612aef215794db8729f75c4c950e79f7fbecd976a08f7bcf1365af1b315002dc025f9b82b2b29fee93afc2286eba46d5981c21d7777fc49b6028026c10792c9e2f80240b27cdfff66100427d9f2561486e1d61a7796eb98ec65eb9a99e7799beb75dfae9869eb008c1e0957008a827e0a00f245fdf22d8a365b6eb9ef1f6dd20f915db8f7c3be47491ace79a2bffb77d03aff76637db95c8e56af5e6d73a51ac23daf7b7eb71db73db75dbb2f00808cf1871f00204f189792c5f1058064a99fbd22105bde8a7cdfff918ec739b6bc9995cbe5cdf49ae72bced5af636c3d80d18be7b3cc6b01e41dfd1400e44b3569c5c5a00935245ce52b3248b81af23cf17dff73f17aa79c728acd936a28f7fcf1b6dc76edbe000032c61f7e00803c615c4a16c7170092a57ef6c12008f6b4e5ad260cc3bd752c96b9ab5cd9ba663565ca9471d50f642f588f642b2031f17c96792d80bca39f02807c8993566aa2df841a12aef21519265c0d789ee8f7abe2fa9b6fbed9e64835947bfe9a7db9aa763f000039e03a685b06004056189792c5f10580e44c9f3e7dacfad997264f9ebcb1ad6b31633a7a6ea97798ad68569ee76daad73b2708829fad47b21590a8783ecbbc16c371c821876c65cb80b4d04f0140bed424add85827a18684ab7c450e12aefa9c275ade1d972f5ab4c8e64835947bfe9a7db8dbec2e00206bae83b665000064857129591c5f00488ee7796f0e82e0afb6bcd5f8be3f4de3cd9fd66b91c4a3aeaeae4df4becf56fca2bbbb7b7d5b0fa0b1e2f92cf35a0c240cc3d7e9fcf882c623d737ff7f5afe4431c1ae07248d7e0a00f2a5266965a0a824d4907095afc851c255ed79f26cfcfbaa55ab6c8e5443b9e7afd9f6d3767f010019731db42d0300202b8c4bc9e2f80240727cdff7d4cffeda96b79252a9b4a5fb0050c7621f5bd78cdcd5ccf45a6f0c82e097245b01e988e7b3cc6b51c35d59f15dea8b4fd272a16285e242f5cfffa3e5ce8a13142b553f4bcb89f6c140525c3f45100441142f48b8ca57d8f7276ff1af7ffdcbe64835947bfe9aedadb5f30d0040c65c076dcb0000c80ae352b238be00909c2008bead7ef6045bde4af4facff47dff7c5bde8c5cb2955eeff57adf2ff33c6f035b0f2019f17c96796d6b2b97cb9ba9ff3d44e7c14f15cb157f567c5765fbf69700ebd6d7f83443eb2cd13a731507da7500004073ab495ab1714b07b714cc6de4e80a57bde789964fc7e55ce10a005a9ceba06d19000059615c4a16c7170092e3fbfeafd4cffab6bc550441f016bdfe155a6e6beb9a8de7791be9b5fe567105c95640bae2f92cf3dad6a3f77c478db59fd3f27ac51ac5ef35e67c51cb9dedba03696b6bdb50cf51eae8b912d642f7b32bb3eb010080e6e3e68f26d649b48a917095afc841c2559ff344bfdf1dd72f5ab4c8e64835947bfe9a7db9bb763f000039e03a685b06004056189792c5f10580e4a88f7dd8f7fdb7daf25651bd62c8e76d79b3993e7dfa58bdd7d7ba043bbea407d217cf6799d7b68431ee16b57aaf4fd0728196cf689cf985961f9b3a75ea1676e5910ac370921bbbf47c4bdcd5afdc55b0ec3a0000a079d424adf449a0a99575c2d58b2fbee8e6b995d860830da29d76da299a316346f4ecb3cff659d7c6430f3d5479dcf1c71fdfa7cec6bc79f3a24b2eb9a44f79de22c384ab01cf13955f15af77f3cd37db1ca98672cf5fb34f57d97d010064cc75d0b60c0080ac302e258be30b00c9a85ef1e825b7b475ad40afdd775f8637fbd59e5c8295fb8053aff56a977865eb01242f9ecf32af6d4e53a64c19a7f776aabb3dad96cb8220f88b96a7b4b7b7bfbbbf5b0536829e7fa2b6334bcb958a13cbe5f278bb0e000028be8e4112686ae525e16afffdf78faebcf2cae8e8a38f8e36da68a368bffdf68b5e7ef9e53eebd7c64812ae0e3ae8a0e8d0430fed539eb7c820e16ac8f3a47ad5d54a12d429a79c6273a41aca3d7fbc2db75dbb2f00808cf1011500204f189792c5f1058064944aa53dd4c73e64cb5b81bb22885efb13eecb705bd74caab7a172b78dbc96642b203bf17c96796df3f03c6f8720083ea3f7f4ba8e9e5b05deecae381586e12e76dd24699b13b4ed998a556ee97eb7eb000080e69797842bcd917acb8e39e6984ad9f5d75f5ff9fdbcf3ce8b76de79e768dcb87195c4ac050b1654ca6dc2d540eb1d77dc7195f5e2b8f0c20b075d3fcb483be16a38dcfc55f3c57fbbbf49cae572b47af56a9b27d510ee793b3b3b2bdb71db73dbb5fb0200c8181f500100f2847129591c5f004886effbea623baeb4e5ad20088293f5da2fb2e5cdc45db94baff10ac575ad7a1533202fe2f92cf3da421b1386e13bf41e762bee553cabb858e349bbfad857d895d3a67dd84efb73826265f5ca5713ed3a0000a079e531e1eabaebaeab949d7aeaa9d1dd77df1d8d1933263ae49043a2993367467becb147b4f7de7b57d6ab4db81a6cbd5b6fbd351a3b766ce5aa59575f7d75b478f1e241d7cf32f29870e5688e789bfb9bc4c5f9e79f6f73a51ac23d6fbc0db73dbb0f00801ce0032a00409e302e258be30b00c950ff7aa2a2db96373bdff777d5eb7ec6f3bc57d9ba66514db6ba5471fde4c99337b6f500d215cf6799d7168bfad24d35667c58efdbb98aa7150f0741709a96efc9ebed68dd151cdd95b6b48f4bb4af731507da75000040f3c963c2d5b5d75e5b2973b7973bedb4d32a3fd7c6faebaf5f795c6dc2d560ebb9e7dc78e38dd7b9a5e050eb6715794db86a6f6f7fbffb9bc445a954aa24ad35927bbe300ce3ab5b456e7b761f000039c0075400803c615c4a16c7170092a1fef52adff73d5bdeecf4ba6f507cc996378beeeeeef5f5fa2e56dc44b215900ff17c96796dfe757676be5aefd3a7353efe46cb352e69c98d199ee7bdc1ae9b67d55bca96b4ef0b5db89f5d995d0f000034873c265c1d71c41195b29b6ebaa972952bf7f3d9679f1dcd9d3bb737d6ae5dbb4ec2d560ebb9e7b4095743ad9f55e435e1cad1dcf07af777898bc30f3f3c5ab3668dcd9baa8b7b9e2f7ce10bff5ffcdc6e3b76db00809ce0032a00409e302e258be30b00c950fffa88e7796fb6e5cd2c088243147f69d62f9d5db2955edf2f14b3bbbaba36b1f500b211cf6799d7e693de97b7f9beff4d2def563ca7b854fd68a07e742bbb6e11856138a99a38b6c45dfdca5d05cbae0300008a2d2f0957fbefbf7f74d9659755127936d86083e8bdef7d6f25f9e99e7beea95c796adf7df78d2eb9e492caedffce38e38cca636b13ae065bcfc5b6db6e1b4d983021bae28a2ba2fbeebb6fc8f5b38a3c275cb97f24a8ce792b89515ffffad7479d74e51e7fdc71c7f55ed9ca3d7fd1fe6101005a0a1f500100f2847129591c5f00683c77e523f5af2f4d9f3e7dacad6b562e0149af7951135fd27e4c10043fd36b9ce36e83652b0164279ecf32afcd07371e84617890de8f1f2b9e54fc4dfde7f7b46c6bd6845c47af6fa25ee72c2d572a4e2c97cbe3ed3a0000a098f29270e562ecd8b1d12ebbec121d7df4d1d1aa55ab7ad7b9e8a28ba25d77ddb5523f7efcf8e8a4934eaa94d7265c0db69e0b7725ab6db6d926da72cb2da34b2fbd74c8f5b38a3c275c39ededed07683ef82ff7f7890b972057efed05dde3cc95adfee59edf6e130090237c400500c813c6a564717c01a0f17cdfdf4bfdeb83b6bc99e9f5762baeb0e54d628cded3f3f5fae64d9932659cad0490ad783ecbbc363b41106cafe37f98e29a8e9e5b05deaae5519ee7ed66d76d761a2f26e8b5cf54ac724bf7bb5d070000144bd60957c4ba91f7842b47f3c04f76d4245d954aa5e8fcf3cf8f56af5e6d73aafae5d673eb8761587b652bf77c9fb4db0200e40c1f500100f2847129591c5f00683cf5ad6147f3261ff5e179deebf57a9f29954aafb5754dc0255bfd44af6f3eb78902f2299ecf32af4d5735b9f878c59f3a7a928b2e57594963c23676dd56a4e3b09d8ec9098a95d52b5f4db4eb0000806220e12a5f5184842ba77aa52b374f8e13a6a272b91c9d7cf2c9d19c3973a2c71e7b2c7aeeb9e72a09566ee97e77e5aebeb3b3b336d1cac52aae6c050005c1075400803c615c4a16c717001a4f7deb777cdfffa62d6f567abdd70441f0355bde0cf43efe48afef76cff336b77500f2219ecf32af4d96bb5d6e188693749ccf513caef8bbfac8efabffff402bdd4277a45cb2ae8ed30c1daf253a56731507da75000040be917095af284ac29553bdfae90d358953f5c40d5c3515000ac475deb60c0080ac302e258be30b008dd7d19380f4515bde8cdc17c77abd7ff33c6f235b5774beef9fadd7f687a953a76e61eb00e4473c9f655edb78e57279bc8e6b97e2d78ae715b7a9dfffaafac737d97531b8b6b6b60ddd15c0740c17ba703fbb32bb1e0000c81f12aef215454ab88ab5b7b7bf5f73c0f9ee6f9611c47cf738fb5c00809c739db82d0300202b8c4bc9e2f80240e3a96f7d340882dd6d79b37149567aad7f6bc6ab75b8abb6e8b5fdb1542a6d69eb00e44b3c9f655edb18eaffdeeaae5aa8e3f907fdbcba7a3bbcf2b469d35e69d7457ddc95c2dcd5ae745c97b8ab5f71cb5a0000f28d84ab7c451113ae629ee7eda039e0673507fc8d9677299eac2657b9e55dd5f2cfbaf5ec63010005c1075400803c615c4a16c717001acbf3bc4dd5b7bed40a57ada87e217f8d2d2f3adff7cfd0ebba5befe52b6c1d80fc89e7b3cc6bebe39267d59f7fb07a55bfc58a458ab3dadbdb0fe05681c9d2719e584d685ba938d15d51ccae030000b247c255bea2c80957008016c0075400803c615c4a16c717001a4bfdeadb7cdf7fc096379b52a9f45abdd6673ccf7bbdad2b32bd77a7ea75dddbd5d5b595ad03904f245c8d5c1004dbaabf9ba66376a5bb8a9596776879accadf62d745f274ec27e83d98a958e596ee77bb0e0000c80e0957f90a12ae0000b9c6075400803c615c4a16c717001a2b08824ec565b6bcd968fcb842d16dcb8b4cafe7bbbeef2ff03c6f1b5b0720bf48b81a1ef56d6fd6313a4671bbe279f577bfd278f571956f67d74536dc7ba1f7e604c5caea95af26da75000040fa48b8ca5790700500c8353ea00200e409e352b238be00d05841109cacbef5785bde4cf41a3fa0d7b8a8abab6b135b57547a3d272a164e9b36ed95b60e40be9170d53f773b401d93ff569ffd032d1f532c51fcd0f7fd0fb9db08daf5911fe5727933bd4f33dc7ba6f76faee240bb0e0000480f0957f90a12ae0000b9c6075400803c615c4a16c717001acbf7fddfa86f3dd496378bb6b6b60d8320f88b5ee7476c5d51e9fdea56fcd9dd62cbd601c83f12aefec35da1cf5d6951c7e28aeaad02ef541c572a95f6b0eb22ffdc98abf7b1a4f770a10bf7b32bb3eb01008064917095af20e10a00906b7c400500c813c6a564717c01a0b1d4af3ee6fbfeaeb6bc59e8b57d59aff1065b5e547a2dc72b1ee4965a4071b57ac2551004bbab6f3e5aaf7fbee279c555fafd13e57279bc5d17c51586e12477b52bbdbf4bdcd5afdc55b0ec3a00002019245ce52b48b80200e45aab7e400500c827c6a564717c01a071a64c99324efdea8b9ee76d60eb9a815ed7abf4fa9e09c3f08db6ae888220f89a5ecf435a6e6feb001447ab255cb92b1cb5b7b7bf5faff74cc5a38a277cdfff91bbeddce4c99337b6eba3b9e8fd9ea8f77a96962b15279258070040f248b8ca5790700500c8b556f9800a00500c8c4bc9e2f80240e38461f80ef5abf7dbf266a1d77651100427dbf222d2ebf8aae2af2e89ccd60128965648b8d2f8b2b55e5fa87eeb322d5729ee527c43e57bdb75d11a7cdf9fa0736066f57c98e97eb7eb000080c620e12a5f41c2150020d79af9032a0040f1302e258be30b008de3fbfeb420087e69cb9b417b7bfbbb35663cd10cb730d27bf415bd96bf295e63eb00144fb3265cb9dbd356fbab798a358a6bf5fba74814452d774b5c9d1b27285656af7c35d1ae030000468784ab7c05095700805c6bb60fa80000c5c6b8942c8e2f00348eeffba7badbd4d9f2a273b748d46b5ba0d7d66eeb8a46afe1488d7d8fea35ed60eb001453b3245cb9be567dd4fbd4df9ea1d7f288e229fdfc132d0feeeaeadac4ae0fd47209d13a5f66e87c59a2f368aebbc5a45d070000d48784ab7c05095700805c2bfa07540080e6c2b8942c8e2f00348efad4eb14536d79d1e9357dc17d796bcb8b46afe18b7a2d8f2976b475008aabc809579ee7bdc2f77ded7ac7258a6715f728bea5b2b7db7581e1686b6bdb50e74f49e7d14217ee675766d7030000c347c255be82842b0040ae15f1032a0040f3625c4a16c717001a477dea62dff727d8f2220b82605bbdae155abec5d61589f6fff37a1d8b3ccfdbc9d60128b6a2255cb971a27ab5bd391d3db70afcad627a07b7394583856138c9254cebdc5ae2ae7ed50cb7050600200b73e6ccf9f7cb2fbfdc27f187483ff43e2c9d3d7bf65afb1e0100901b45f9800a00d01a189792c5f10580c6f03c6f73f5a92f767777af6feb8accf7fdf3f5baceb4e5451204c1675c329c62675b07a0f8f29e7055bd55e0feeeb6b3dac787144b15e7a96c8aea36b5eb038da6f36da2ceb7595aae549c582e97c7db750000c0c0e6cf9fbf74c58a157d927f88f4e3f1c71fbf7cf6ecd977d9f7080080dcc8eb07540080d6c4b8942c8e2f003486effbfba84fbdd7961759f5353d5d2a95b6b475451104c1a7f41a1ef73ceff5b60e4073c863c295eb37d5877adaa78b14cf28ee537ff46d97f8a2ea31767d200deeea6a3a07672a56b965b35d95130080a4cc9d3bf723f3e6cd5bb36cd9b267b9d25536a1e3be6cc9922597ce9e3dfb09c5c1f63d02002037f2f4011500008c4bc9e2f8024063a83fed725facdbf2021ba3d77397a26c2b8ac2f7fd4f68ff9fe00b65a0b9c5f3d9ace7b52eb153fb70441004b3b55ca3bee7775a7eb6542abdd6ae0b6449e7ea763a374f50acac5ef9ca250202008041b8241f776525c53f1511917ab8e3ee8e3fc95600807ccbfa032a00006a312e258be30b008da1fef474c531b6bca8aa5786ba7dbd825e89c5f7fd69daff27c3307ca3ad03d05cb24ab872b790d536f7537f79b2960f2a96292e50fff3912953a68cb3eb0379532e9737d3f93a43e7ed129dc7731507da750000407e690c3fdecd81dbdbdb6fb37596c6f9d569cf97010068490cb800803c615c4a16c717001a43fde9f5beef7fd896175118865bebf52cd7726f5b57044110746aff9ff23c6f375b07a0f9a499703575ead42dd4c77c54dbfab962a5e27ec589ea2fdfb95e41135481b6b6b60d358729e95c5ee8c2fdeccaec7a0000203f3427fd9a9bffbad0d87daeadafa5758f8cd7753fdb7a0000d040697c400500c070312e258be30b008da1fef471773b295b5e447a2de7f8beff235b5e048168ff9fd662775b07a039c5f3d9a4e6b56118be4e7de2e17afe9b146b1437a88ff9bcfafc9decba40d1e97c9fe4ae76a5f37c89bbfa95bb0a965d070000644be3f431710255354eb7ebd48aaf6ee5c2fd6ceb0100400325f501150000f5605c4a16c7170046cf5df144fde90beb35c1d54d7cdfdfaba3e7ea565bdbbabc0b82a05dfbbed4f3bc37db3a00cd2b9ecf366a5eeb6e15a8bef0bff47cdfd1f2012d57a87ff9999687aa7fd9dcae0f34239def1375decfeae8b992db89e57279bc5d070000a44ff3d3a3ddbcd7c41576bd58edd5ade2e02a57000024c80db6b60c0080ac302e258be30b00a3a7bef45d8abb6d7911e975dc1604c1a76c79de69bf3fa658eafbfe5b6d1d80e616cf674733af7557f1d1e30fad2656ada8265ab984abff720958767da055a80d4c505b98a958e596ee77bb0e00004887c6e12fbb39af0d952fb0ebc66aaf6e150757b902002041a3f9800a008046635c4a16c71700464f7de927153fb7e545a3d75056dc55b4e402edf3a18a654110ec69eb0034bf783e3bd279add6dfd1dd1a50cb1b3a7a6e157853f5d6813bdb758156e779de766a1b27285656af7c35d1ae03000092d3df95aa6ae27fd7ebe78adb833d86ab5c0100901037d0da320000b2c2b8942c8e2f008c5e1004df7397f5b7e545522a95b6d498f0741886efb47579a67d9edad1730bc4bd6d1d80d610cf678731af1de3fa38ad77a2e2fe8e9ec4915f68f931776b58bb3280bedcd5e034e799a176b344ed67aee240bb0e0000682c8dbb47c489520385e7793bd8c7f57775ab38b8ca150000097103ad2d0300202b8c4bc9e2f802c0e8f9be7f63188607d9f2227149631a132eb0e579a6e3fe61edf3722ddf6eeb00b48e783edbdfbc76ca9429e3d4477cc4f56f1d3d57c2fb8b96a7b4b7b7bfbb6857f303f2a4adad6d43b5ad92dad34217ee675766d7030000a3a739ecb61d3d57d6be49f17f6ede6b43f3dbf79bc70c7875ab38b8ca150000097083ac2d0300202b8c4bc9e2f802c0e8a92f7d320cc3d7d9f2a2f03cefcd7a0d2bdded826c5d5ef9be3f59fbbca2835b1a012d2f9ecfc64bf7dffdfaf9b3ea277ed7d173abc09bdd1579d44fefb2ee23013482dad62477b52bb5b525aeadb9ab60d9750000406368dcdd5a63eea73b7a12affee9e6c0d5985ebbde6057b78a83ab5c0100900037c8da3200402e8cb305ad807129591c5f00181dcff35e1104c13ff4e3185b57141a0be6f8be7fb82dcf2bf7c5aef67945d16e7f0820196e3eabfee01dd52f8eee533cabb8587d73bbbb5daa5d1f4032d4ee26aaddcdd272a5e2c472b93cdeae03000046cf5dc1d5253bbbb9aec6dcb2e21afd7e925dcfe27360000052c0800b00b9f43ec58aeab2a5302e258be30b00a3a37e743fc59f6c7951f8beef69ffeff73c6f035b974741107c50fbbb42fbfd5fb60e40eb509fb569f5b6a2e7baf9ace2e1eaf23d45e9cf8066a5b639416d71a662955bbadfed3a0000a07e1d3dff5cf0795b3e143e07060020050cb800903b71b2d551d5654b255d312e258be30b00a3a37ef4d34110fccc961781bbe58ff6ff09edfffeb62e8fb49f1f70c956edededefb675009a9fdaff6b5c9febfbfe6fb45c53bd85d997e2f92cf35a205fdcad8ad52e4f50acac5ef98adb000300304a1a5f37d298ba4acb57d9baa1305f060020050cb800902bf6ca56f6f7a6c7b8942c8e2f008c4e10043f507cc5961781c680ef2a2eb6e579a4fd6c735fd82ade63eb00342fb5f9b7f9beff4d2def563ca7b844bf777475756d55b30e0957408eb9046fb5db196aa34b5ca2a4e240bb0e0000181e8da7076b2cbdd5960f07f365000052c0800b00b9315072d540e54d897129591c5f00189d20086617f18bc3300cdfa831e099cecece57dbbabc696f6f7faf4bb6d2716e89b90fd0cabababa3651ff7490dafc8f154f2a1ef17dff0cd7fedbdada36b4eb3b245c01c5e0dab0da73496d75a10bf7f340ed1a0000f44f63e8859a1b7fd1960f07f365000052c0800b00b9305452d550f54d837129591c5f00181df5a34b153bdaf2bcd33e5f5f842b73b9db0776f4dc46f0fdb60e407370b743513b3f4c718d628d629eeb9f7cdfdfd5aedb9f783ecbbc16288e300c27556f0bbac45dfdca5d05cbae030000d6357dfaf4b11a3b9fd5fc79075b371ccc9701004801032e00646eb8c954c35dafd0189792c5f10580fa8561b8b54b0eb0e579a77d9e1a04c15ff27e5509ede3bed564ab036c1d8062f37d7f2fb5efe3157f52ac527bbf4ccbd0f5ab76dda19070051497daed44b5ff591d3db70d3eb15c2e8fb7eb0000801e2e6159e3e51db67cb8982f03009002065c00c8d44893a846ba7ee1302e258be30b00f50b82607ff5a377daf23c73b7ebd23e2fd2be7fc0d6e589f6f15d8a15beef7fc8d601289ec993276fec6ebfaa36fd23b5ed27148f2ace7457af1b6df2673c9f655e0b1497fa86096ac33315abdcd2fd6ed70100a0d5697c3c5fe3e4976cf970315f060020050cb80090997a93a7ea7d5c21302e258be30b00f50b82e033ea472fb0e579a6fdfd96bb92842dcf13dff7f7a9265b4db675008ac35da946edf8136acf57299e57ccd7ef47ab0fdaddae3b1a245c01cdc3f3bcedd4964f50acac5ef96aa25d07008056a4317203373e6ab993ad1b2ee6cb0000a4800117003231daa4a9d13e3eb7189792c5f10580faf9be7f76c728febb346ddad79d15cf2876b475791186e13bb47fcb1507db3a00f9572a95f650fb3d4e71a7fac8d55a5e110441e7b469d35e69d76d94783ecbbc16681ee5727933f52133d4ae97a80f99ebae9067d70100a095684cfc6fc59f6cf948305f060020050cb80090ba46254b35ea7972857129591c5f00a89ffad03945bae59df6f56a970861cbf342fbf6b68e9e64aba9b60e403e799eb791eb07d56e7fe81223148f0541f003f785d0f4e9d3c7daf59310cf6799d702cdc7dd72547d4c49ed7ba10bf7f3686f430a004011691cfcb1bb5aac2d1f09e6cb0000a48001170052d5e824a9463f5fe6189792c5f10580fab9e420cff376b0e5791486e124edefdf5c7284adcb03dff7f7d2fe2d0b82e0105b07205fd44eb7557c5cedf657d5ab58dda1e5b12a7b8b5d370d245c01adc1cd65dcd5aed4d697b8ab5fb9ab60d97500006846ddddddebbbcf1f3416ee62eb4682f93200002960c00580d4d8e4a8318a8f579723611f679fb7d0189792c5f10580fab8db63b944035b9e472ec94afdfd23dadfc9b62e0faab72073c9561fb57500f2c12553b9a4aa6a72d56a976ce592ae5cf2955d376d245c01ad456d7da2fa9e595aae549c582e97c7db75000068261af7dea731ef5e5b3e52cc9701004801032e00a4c22645b964a99f2a5c1fec96c34dba1ae871f6f90b8b7129591c5f00a84f7b7bfb7b5de2812dcfa36a92c4b5b63c0f5c1287f66da9f6d1b37500b2e36e07a87eee00b5cfb3148f29162b7ea8b6faa1bc5d292f9ecf32af055a8bfaa3096af73315abdcd2fd6ed70100a019b879b8fe76fe9a2d1f29e6cb0000a48001170012d75f32d4c7d7eb499a8a63384957b5c956714caba9ef6f3b85c3b8942c8e2f00d4c7f7fdcfa90f3dcf96e74da9547aadf6f319cff35e6febb2a663f826eddbd35a76d83a00e97357ee0b82a0d35d39a67aabc03b15c7b9abd0d975f384842ba0b5698eb39ddaff098a95d52b5f4db4eb0000506063dcdfcd6118bed1568c14f365000052c0800b00891a2809aabfe4a9c192aefa5bff826a79ad81b657188c4bc9e2f802407dd47f9ee3fbfe0c5b9e37dacfcb8320f8b62dcf9ae779bb69df9ed2312cd93a00e951ffb0bbdae1d16a8ff315cf2baed2ef9f28d22dba48b802e0a8dfdacccdcdd4172c51df365771a05d070080a2696f6f7fb7c6b685b6bc1ecc9701004801032e000c2e0882d58a236df9300c95fcd45f12557f4957fdadd75fb2556ca8ede61ae352b238be00501ff59fb7b8db6dd9f23cd1febd5ffbb9d8f3bc4d6d5d96dc7fe66abf9ef47dbff6ca9c0052d0d6d6b661b56f3853f1a8e271b5c51fb9c484c993276f6cd72f0212ae00d472fd9c4be8765f4ebb703fbb32bb1e000045a071ecfb1acfbe61cbebc17c1900801430e002c0e05c3fe962848957c34d7aea2f99aa36e9aabffac192ad62c3dd7eee302e258be30b00f551ffb9b2b3b3f3d5b63c2fdc178bdac707355739c4d665c9f3bc3768bf9e5074d93a00c908c3706bb5b950fdc1655aae52fc4971bceffb7bd9758b289ecf32af0560a9ff9be4ae76a5fe6189bbfa95bb0a965d0700801c73b7137c4263d89b6c453d982f03009002065c00189ceb276b63188957234d76ea2fa9cafdbe7e3fe5c349b68a8d743f72817129591c5f001839cff3b653fff99c2dcf13eddf977cdfbfd19667290cc35db45f8f2b0eb375001a4bed7f57fd8df215b5b7798a358a6b5cdb53fff52abb6ed1c5f359e6b50006a2fe61a2fac4595aae549c58a4dba602005a97c6ac7769fcfa8b2daf17f365000052c0800b008373fd647f3140e255bd494efd255d3d627e1f49b255acdefdc90ce352b238be003072ea3bdb14b7d9f2bcd07c647beddf332ee1c2d66545fbb3b3628962baad03307aeeaa766afbef53bb3f43edec11c5938a1f876178505757d72676fd6612cf6799d702188afac809ea2b6676f45ced6fa6fbddae0300405e68ac3a5d73fc6fdbf27a315f060020050cb8003038d74f0e16358957a34d6eea2fe96a34c956b1d1ee57aa189792c5f105809153dff905dff77f62cbf342f3905f681f4fb1e55909c3f075da9fc53a669fb37500ead7d5d5b595da959a57c7258ae71477ebf76f6af936bb6e338be7b3cc6b010c57f56aa527285656af7c35d1ae030040d6dcdfd1a552690f5b5e2fe6cb0000a420eb01d76d9f2008a2e8e1795eb4c9269bb8fef428dbcf8d90bb8da0bdb295fbdd958fc651769f89d60e7b82000006a67e736610045fb4e579a07ddb4ff164b95cdeccd66541fbb2a3e231dff70fb77500464e7f67bc416dea4bea83e66ab9466deb375a7ebab3b3f3d576dd5611cf6599d3021829375f523f3a43fdc712d7af2a0eb4eb000090058d4d1315ee7b808661be0c00400ab21e70b3de3e80fab54afb75afb3bfe8e79682ef5b6f7457921aec0a57ae9c2b5ca12138c60030321aef6f557cc09667adbbbb7b7df5e9f7297c5b978552a9f45aedcbdfdd1799b60ec0f0789eb781dad17bd4e79ca6e5c38aa715e7aa5d7d58759bdaf55b513c97654e0ba05eeeb6acea574bea4716ba703fbb32bb1e000069d178748ae23bb67c34982f03009082ac07dcacb70fa07eadd27eddebac8d7e12ad6ad59bdcd45fb295bdd2553d4957f5ee4f665ae5bcca12c718004646fde6b31afbb7b7e559d33e7d5efb768b2dcf82f6e3358abff9beff655b076070a552694bb5e776b5a18b5d7fa3b857d11d86e13bd61bf9fcbfe9c57359e6b4001a417deda4ea550497b8a4f1bc5c351400d05a340e3ddad1e05b85335f06002005590fb8596f1f40fd5aa5fdbad7e9628844ab5a234d72ea2fd9ea82f57a6e2368cb47927435d2fdc8855639afb2c4310680e1738956ea379fb1e559d37e6dabfd5aa1e55b6c5ddadcadcdb42f8ff8be7fb4ad03d0bf300c77a9ded6ea66c51ac5756acf9ff13c6f07bb2ed615cf6599d3262f8aa28d162d5a34ebce3beffcbf3973e644b367cf26520e1df77fcf9f3f7fe9dcb9733f62df1f3496fa9489ea876769b9527162b95c1e6fd7190a6d26fba0cd002822fd6db0b7c69ec76cf968315f06002005590fb8596f1f40fd5aa5fd8e20d1aad670939d064ab68a93aafaab1f4ed2d570b79f3bad725e6589630c00c3a739c007d46fceb3e559d33e9de7fbfef76d79daaa09690f6b5f8eb57500fec3dd02b4bdbdfddd6a2fa7a8ddfc45cb658a0bd4763e3265ca9471767d0c2c9ecb32a74ddea2458bae9c3f7f7eb462c58ae8e5975f8efef9cf7f1229873beeeef8cf9b376fcdecd9b30fb6ef111a4ffdf204f52f3315abdcd2fd6ed719086d26fba0cd002822fd7d7092c69b536df968315f06002005590fb8596f1f40fd68bf431a2ae9a9bf64aada64ab587feb0d967435d476738df32a791c630018be2008bee8fbfe8f6c7996d48f4f542c75b721b3756972577e7089233a3e5fb77500d65b6fead4a95ba8ad7e4cede4171d3d574cb95f71621886ef5c6fe0b93c8610cf6599d326cf5da5c7252ed8840622fd58b66cd9b3b367cfbecbbe47488ee779dba99f39c1f5dfd52b5f4db4eb58b499fc046d064091689cf9abfeaedec7968f16f36500005290f5809bf5f601d48ff63b2c03253ff59744d55fb255acbff5fb4bba1a687b85c179953c8e31000c9feffb3f0982e0f3b63c4363d48fff49fb35cd56a4a9fa25e49f15dfb275402b539bd8d9256a6af9fb8e9e5b05dee0fa102d77b4eba23ef15c96396df2dc2dd1b84a4f3ec2bd0fb367cf5e6bdf2324af5c2e6f56bd05ec12f5e7731507da7562b499fc046d064051688c79ab1b636c7923305f06002005590fb8596f1f40fd68bfc3d65f12d4c7d71b7eb255acbfa4abda2f5bfbdb4ee1705e258f630c00c3a73ef3b620087233b66a7f0e53dcb1ded0f386c44c9b36ed95da87858a136c1dd06adcad027ddfff2fb587ef76f424212e579ff1332d0f755fd2dbf5317af15c96396df266cf9edd278981c82edcfb61df23a4a7adad6d43f5f7a5ea1c68a1fbd995d5ae439bc957d066001481c6946efdfdf03d5bde08cc9701004841d6036ed6db07503fdaef88d864a8dae4a9e1245bc5067a9c7dfec2e2bc4a1ec71800864f7de6aa2008b6b5e559e8eaeada4afbb34cf1365b9716cff3b6f17d7f81f6e13bb60e68156a079bbb84aa6a62d50ab589075c9b7089572e01cbae8fc68ae7b2cc69933792e49165ab5e88be7cd9fd95703fdb7a62f441f2487e846138c95ded4afdd01277f5ab38c1963693afa0cd0028028d250f6a4cd9d7963702f36500005290f5809bf5f601d48ff63b623629ca254bb92b540d37d92a661f679fb7d038af92c7310680e1e9ecec7cb54ba6b0e559d1befc5031d396a7250cc3adb5fd7b15a7d83aa0d9799eb753f5d6803774f4dc2af026dff70f57bb789d5d17c98ae7b2cc69933758f2c8dab5ff8c9efbc74bbdbfcf7f78796ff288fb392e5ff9fc8bd1f3fffb9ff588fa83e491fc513f345163c32c2d572a4efced6f7fdbe77d8b8336937ed06600e49dc690dd357e3cb9dec8bf1f1816e6cb0000a420eb0137ebed03a81fedb72e8d4e8e6af4f3658ef32a791c6300189ef6f6f603dcd50b6c79167cdfdf4bfdf7727785295b9786ead5b5ee569c6eeb802635260cc377ba2fd015f757bf4cffb9fa848f4e9d3a750bbb32d213cf6599d3266fa0842b9738f2cb3b16475fbbf281e8f77f5e1acd7b6859f48dab1eec4d1e713fbbf2df2d782a3a56eb9c79e323d19a174820196d903c925f9aa74d509f34f3b0c30e8b5e78a1efd5aa6833d9046d0640de69ec385e71962d6f14e6cb0000a420eb0137ebed03a81fedb76e8d4a926ad4f3e40ae755f238c600303cee1631ea337f68cbb3a0fd98af986ecbd3502a95b6d4b6ffa438d3d601cdc4dd124aedfe233ad72fe8e8b97da7bbbdc7c95aeec7ad02f3239ecb32a74dde400957cfae79313a66d603bdc92243c551972f8c9e7cf61f7d9e871859903c927f37de78639ff7cd056d269ba0cd00c8bb8e9e7fec788f2d6f14e6cb0000a420eb0137ebed03a81fed7754469b2c35dac7e716e755f238c600303cea2fcf537cd696a7cdf7fd92f6e3ee2c123edc957cb4ed3b3b12fcaf5b204ba552e9b5ae9dab9dfd4ecb354110ccd6f208cff35e6fd7453ec47359e6b4c91b28e1cac5f5f73fdd274964a098f5a7c7fb3c9e1879903c927fb4997c056d06409ee9ef8d37683ebb34c9bff3992f03009082ac07dcacb70fa07eb4df51ab3769aadec71502e755f238c600303cea2fefe848f0bf4d87a39af0f494e25db62e699ee76d5e3d06e7d83aa0c0c6e89c9e1804c1b7b5bc4ff18ce222dff73d773537bb32f2279ecb32a74dde60c923ee96687172c8d1572c8c2efdc392e8b1e5cf57c2fdeccae2fa1b163edde7f1c4c883e491fca3cde42b683300f24c7f7f1cabf8912d6f24e6cb0000a420eb0137ebed03a81fedb721469a3c35d2f50b87f32a791c6300181ef597cf7b9eb78d2d4f93effb67683f7e6acb93e66eada6eddea6f8b17e1d63eb8122513bde340882291d3d57ad5baa78486deb5495edafba0decfac83712aed2d35ff2c8b2552f44f31f5e1e7de3aa077b93435cb2885dcf95c5f56e5df718f758bb1e31fc207924ff6833f90ada0c803cd35cf69ef6f6f6f7dbf24662be0c00400ab21e70b3de3e80fad17e1b66b84954c35dafd038af92c7310680a1799eb783facb65b63c4dbeefbf49fbb052fbb29dad4bd2942953c669bbf35c72ca7a245ba1a074febe46315df15bc51ac59c20088e54bb9a60d745b1c47359e6b4c9eb2f79244e08a90d77851ebb9e2bb3ebb9b0eb11c30f9247f28f3693afa0cd00c82b77fb72cd655724fdcf1fcc9701004841d6036ed6db07503fda6f430d954c35547dd3e0bc4a1ec7180086e6fbfe87d45fde6ccbd3e4b61f04c1176d7992aa57029adbd173552d92ad50286ab76fd7b9fb2dc53d8a67752eff524b5fe7f52becba282e12aed243f248be82e491fca3cde42b683300f24af3d8a3f4b7cb4f6c79a3315f06002005590fb8596f1f40fd68bf0d375052d540e54d89f32a791c6300189afaca2f29ceb2e569d1b63fa65898f47fbcd672c956dae6cd8a9f777777af6feb81bce9eaeada44e7ebc1eecb0a2d9f523ca238bdbdbdfdbd69b61da42b9ecb32a74d5e7fc923dc1e2dbb207924ff6833f90ada0c80bcd23cf68ffa9be5005bde68cc9701004841d6036ed6db07503fda6f226c7295fdbde9715e258f630c0043535f798162ba2d4f43f5967e8f2bde63eb92524d5cb9497131c956c833cff35e1504c1a774ae5edbd173abc079fafd2bbeefef6ad745738ae7b2cc6993d75ff2481cf31e5ade9b1c72f4150b2bc922ee0a3d2edccfae2cae77ebdac713230f9247f28f3693afa0cd00c823cd6177543cd3d6d6b6a1ad6b34e6cb0000a420eb0137ebed03a81fed37317192d551d565cb245b399c57c9e31803c0d03a7afee3f4ddb63c0ddaf677dcadd06c7952264f9ebcb1b67983e212ae0a843c0ac3706f9d9fdf50dca558a5f67199962a0eb7b6eba2f9c57359e6b4c91b2c79e4c6079eee4d0e192aae5bf0549fc713230f9247f28f3693afa0cd00c823fd2d73a4e6b117d8f224305f06002005590fb8596f1f40fd68bf898a93ae5a2ad9cae1bc4a1ec7180086a6be724d5757d756b63c699ee7bd41db7ea6b3b3f3d5b62e09dade46dade758acb49b6425eb82440dff727ebbc9ca97842f1a8e2ccf6f6f6f7a7f19fe0c8b7782ecb9c367903258f3cbbe6c5e86b573ed0274964a0f8eaac85d1cae75fecf33cc4c882e491fca3cde42b683300f24873d8db832038d0962781f932000029c87ac0cd7afb00ea47fb4ddc385bd00a38af92c7310680c1799eb793facaa76d791a7cdfff9db6edae7299b8e9d3a78fd5f67ea3ed5d49120bb2562e97c7eb7cfc84e26a9d93cf2be6ebe7a38320d8ddae8bd646c2557a064a1e79f1a5b5d18fe73e5ab905dad5f73c11ddf2d0f2e81b573dd89b2ce27e9ef7d0b2e8f77f5e5a4932f9d9fcc7a297d7aeedf33cc4c882e491fca3cde42b683300f246f3d7d7289e737f8bdbba24305f06002005590fb8596f1f40fd68bf4802e755f238c60030b8ea95757e6fcb931604c1146df7a1343e7cad265bb9c4965f936c85ac944aa53d740e1ea7b853b14a7185da41a7e779dbd87581583c97654e9bbc8192475cb80492e5ab5fe8fd7dfec3cb7b9347dccf71f9aa7fbc44e2488382e491fca3cde42b683300f2467f831fae39eccf6d7952982f03009082ac07dcacb70fa07eb45f2481f32a791c6300189cfac9a37cdfffbe2d4f525757d726daee63ededed07d8ba467309562ed14a714d1ac95d40ccddc2526deb433af77ea858e2cef920087ea0e57f732e62b8e2b92c73dae40d963c6263d9aa177a9347dccfb69e187d903c927fb4997c056d0640de68fe3a4f7f0f7dd8962785f932000029c87ac0cd7afb00ea47fb451238af92c7310680c1a99fbc3008824fd9f224f9beff4d6df74a5bde682ed94aaf6d96bb95a04b7eb1f540a3e93cdb4ee7dcc775cefdaaa3e75681b72b8e51f99bedbac0709070959e91248f10c907c923f9479bc957d06600e489fe26da5ef3d75593274fded8d62585f932000029c87ac0cd7afb00ea47fb451238af92c7310680c1a99fbc2b08827d6d7952c2307c9db6f98c62475bd7489ee76da06d5ceefbfeef48b64292d47edea2f3ec589d6f7768b95acb2bb59ca6f26dedbac0489170951e9247f215248fe41f6d265f419b0190279abb7e567f0ffdd2962789f932000029c87ac0cd7afb00ea47fb451238af92c7310680418d513ff9bfa552694b5b91146def2adff7bf6ecb1ba9bbbb7b7d6de712c50d69fe472d5a83bb1da0bb1da6ceafb3148b148b754e9f1d04c10749ee43a39170951e9247f215248fe41f6d265f419b0190279abbdeacbf8f0eb1e54962be0c00400ab21e70b3de3e80fad17e9104ceabe4718c016060ea2377563c69cb93e2fbfe87b4bd47934c82aa265b5da4f87d5757d726b61ea8c7b469d35ea973aa5cbd45a5bb8ad51ff4f3d7f4f35bedba40239170951e9247f215248fe41f6d265f419b019017ee4abfee6fa6b4ff1e67be0c00400ab21e70b3de3e80fad17e9104ceabe4718c016060ea230ff67dff465b9e0477e59f2008fe1a86e141b6ae51aac956172a6ed6f636b5f5c048a86dbc49e7ec57753edda6785ef16b4557b95c1e6fd7059242c2557a481ec957903c921df537b728da6cb9459bc957d06600e485fe86fa94c691cb6d79d2982f03009082ac07dcacb70fa07eb45f2481f32a791c630018984b26517ccf962741fdf131beefffc6963790bb3de2057a3d73a74c9932ce56024371b70ad4f9f3019da7dfd7b9f477c5e38a73c2309c94e455d980c19070951e9247f215248f64c7f537d51834f18a3693afa0cd00c80bf74f5d1a3f3e66cb93c67c19008014643de066bd7d00f5a3fd22099c57c9e31803c0c08220f885fac94fdaf246f33c6f076de799300c77b1750de292adced3ebb9b55c2e6f662b8181e8dcdcc6f7fd92ce9fcb15ab147fd4ef5fd7b9b4a75d17c8423c97654e9b3c9247f215248f64c7f53726fa4dbca2cde42b683300f2407ff36fad31e3f92cfe098af932000029c87ac0cd7afb00ea47fb451238af92c731068081a98fbc47f12e5bde6841105ca6ed9c60cb1bc4255bfd58719be7799bdb4ac0d279b29bce97a35c829e966b14d7283ea9dfb7b7eb02598be7b2cc699347f248be82e491ecb8fe66805827f18a3693afa0cd00c8038d135dbeefffca96a781f932000029c87ac0cd7afb00ea47fb451238af92c731068001b944a517a64e9dba85ad6824f7c59c62b1e7799bdaba46d0739fa3b823e9d781e26a6b6bdbd09d87eef6995afe4df1a4e2c761181ed4d5d5b5895d1fc893782ecb9c3679248fe42b481ec98eeb6f86884ae2156d265f419b0190071a1faef37dbfc396a781f932000029c87ac0cd7afb00ea47fb451238af92c7310680feb9dbfba98f7cdc96375235d1e5cf8a436d5d23e879cf52dc592a95b6b475686d5d5d5d5b05a2f3e352c5738abb7ddfffa6966fb3eb027916cf6599d3268fe4917c853be7897c076d265f41c21580ac799ef70a8d0f6bb2baf2b41b9b6c19000068b0ac07dcacb70fa07eb45f2481f32a791c6300e85f100453d4475e6fcb1b49db3852dbb8c99637829ef74cc55dee435d5b87d61486e11b7ddfff7247cf9537d6e8e7df68f9e9cecece57db7581a288e7b2cc699347f248be82e491ecb8fe668070e36b5bbc1e6d265f419b0190358d1165c535b63c2dcc9701004841d6036ed6db07503fda2f92c079953c8e3100f4cff7fd63d5479e6ecb1b250882edf5fc2b3dcfdbcdd68d96db6fc53dee2a46b60ead43e7d6063a0fdea373ed34c55ff5f3d38a73756e7f38a95b5802698be7b2cc69933758f2c84b2fbd149d72ca29d1eebbef1e6db2c926d12b5ff9ca68ca9429d1cb2fbfdc67ddac62debc79d125975cd2a77c383192c73ef4d043ee5c8c8e3ffef83e758d0c9247b2e3fa1b13eb245ac5066b3344fa419b0190358d15d7eaefb24e5b9e16e6cb0000a420eb0137ebed03a81fed1749e0bc4a1ec71800faa7fef16245972d6f143df7cf7ddf3fd5968f969ef714c57d61186e6debd0fcdc15cd822068af9ebfcf2aee5574eb7c7887aac7d8f581a28be7b2cc69933758f2c861871d564932fa9ffff99f4a62d219679c111d7becb17dd6cb320e3ae8a0e8d0430fed533e9c18c96349b86a7eaebfa946bf8956b1c1dacc48e2c5175fac9c532ece3ffffcdef2238f3c32da6cb3cdfaacdf5fd8a4c1a79e7a2ada6aabada2534f3db5f2fb05175c10bde635af8936de78e3e8f39ffffc3a75f5c450dbcb22683300b23475ead42d3a7a6e2798d915a8992f03009082ac07dcacb70fa07eb45f2481f32a791c6300e89ffac7fb7cdfdfc79637421004fbeaf99ff43c6f735b371a7ade93f4bcf7eb79b7b175685e6118eea2737586defb9bdd87f88aeb742e7c46e7c10e765da0d9c47359e6b4c91b2879e4ef7fff7b3466cc98e8831ffc609fba38ce3befbc68e79d778ec68d1b17edbffffed182050b2ae5717292bb3ad65bdef29668f3cd378fd47f55ae98359cc7fdf0873f8c76dd75d768d2a44995f2193366549246dc55b6f6d9679fe8de7befad941f77dc71bd092b2e2ebcf0c2419fbf36067aeca5975e1aedb6db6e956d4d9c3831bae38e3bd6d9379770b576eddaa8b3b3b3b2cecd37df3ce836873a1636481ec94ec7108956b181dacc48a336e1ea55af7a55b46ad5aa4af94812aefa4b1abce5965ba265cb96557e7ee31bdf58693beebc7ef0c107d7a9ab2786da5e16419b0190258de981fb3bcd96a789f932000029c87ac0cd7afb00ea47fb451238af92c7310680bebabbbbd757fff862b95cdeccd68d56f5b95d325787ad1b8d2008beade77c60dab469afb475682eee1c6a6f6f7fb7cea353f4beff45cba57aefcfd772ea942953c6d9f5816646c2557a064a1ef9d5af7e55490439fbecb32bbfaf58b1a2375cc2d1dd77df5d49c83ae49043a2993367467becb147b4f7de7b57d68d938cb6dd76dbca1576dc15b2dcefd75c73cdb01ee7924d4e3ef9e4e8f6db6faf94bbc79d7beeb9d159679d156db1c516d1e4c9932be5b7de7a6b3476ecd868bffdf68baebefaea68f1e2c5833e7f6df4f7d8fbefbf3fda70c30da30f7de843d1c5175f5cb995a27b0dab57afeeddb76f7ef39bd111471c116db0c1069563e49e6bb06d0e762cec3eb9207924ff066a33238d38e1ca2530b9a54b0274e536e16aa024c0fe92066b13030f3ffcf0ca7919d7fffad7bfeead738fffe94f7f1a4d9830a172f52b9798f5d7bffeb5523e9204477be5b781f675a489872309da0c802c69aefaeb8e04afa03d1ccc9701004841d6036ed6db07503fda2f92c079953c8e3100f4e579de1bd43f2eb6e58de0fbfee7f4dcf36cf968e839bfa9e77c50fbbd9dad4373a8de82e2634110fc42cb67f49e2fd0f284ea55d8b855205a160957e9192879e4da6bafad24487cfffbdf8fd6ac59b34ea2c5d34f3f1d9d76da69eb94b9587ffdf52b49247172c5e73ef7b9da8488e89c73ce19d6e3a64f9fdebb1f2ec1eb80030ea82442c5ebefb5d75ebdf52e59a4f68a3b833dbf7d8df6b1eeb5baf5dd6dd3dcef2ec1cbfd7ed34d37f5ee9bdbb65bd6de5a71b06d0e762cecfed4d423c7066a33238d38e1ea7bdffb5ee56a6e9b6eba69f4d8638f455ffad2977a13ae064b02ec2f69b03601ca5d796abbedb68bdef4a63755eaefbaebaede3a9724e8ced1f7bdef7d955b04767777472fbffc72659b234970acddde60fb3ad2c4c391046d064056dc3f7269aefa7c18865bdbba34315f06002005590fb8596f1f40fd68bf4802e755f238c600d097effb1fe948e072ffeeea537ade157afeb7daba7ae9b9beeeae72542e97c7db3a149bce959df5de7e51cbdf77f4dc2af0fa6ac2de8e765da0559170959e8192471e7ef8e14a42c4473ffad1ca15ade6ce9d1bb5b7b757ca5cc2d5a9a79e5af9d95d01cbd5c5e1d6b557bd71e5ee7777abc0913cce459ccce4aefae3f6d5ddb66fcf3df7ecadb74953833dbf7d8df6b1a79f7e7ae5b12e51c5fdfe831ffca0f7f778dfdc957976d9659768871d7688962e5d3ae436ed6baa3d16767f5c903c927f03b59991469c70f59def7c275ab870612559c9f5792e992f4eb81a2c09d0fd6ecf617bbeedb4d34e9504295b17b72b773ed6eed348131c6b9f73b07d1d69e2e148823603202bfa1bce53dc68cbd3c67c19008014643de066bd7d00f5a3fd22099c57c9e31803405fea1b8ff37dff545b3e5a7ade738320f8812daf97f6f1583de7c37aceed6d1d8ac7dd2a50efe5be7a4fbfabf8b362b9e2a72a3b2489db5b02cd209ecb32a74dde60c9231ffef0872b49115ff9ca57a22bafbc323af0c0032bbfbb84ab7beeb9a772859c7df7ddb772851c772bbd33ce38a3f2389bf4519b643492c7b98893998e39e6984a42944b04a94db87257cc71b745bbe28a2ba2fbeebb6fd0e7b7611feb925edcad023ff8c10f56aed0e36e8db6fdf6db47ab56adeadd37775bb5050b16546e9976f0c107579e67b06ddad744c255f10dd666461271c2d549279d54f9dd2523b95b00ba2b4ac50957832501badf074b8072bf0f9470e5cecfdae78963a4098eb5cf39d8bedafd1aaa1d8c24683300b2a279ea15fa9bee53b63c6dcc9701004841d6036ed6db07503fda2f92c079953c8e3100f4a5bef112dff7a7d9f2d108c3f01d7adea59ee7bdc2d6d543cf7594e291cecece57db3a1487ce87cd75aefd8fdecb0b152b140b83203849cb77adc7ad028121c57359e6b4c91b2c7964f9f2e5d1c73ffef14a6292bb95d8f8f1e32bc9482e01c9d55f74d145d1aebbeeda5b17278e0c955c31dcc7b978eaa9a7a277bef39d95440f77dbb5193366ac9300e2ae2ab5cd36db445b6eb96574e9a5970efafc36fa7bec2f7ff9cbca15acdceddddc766fbffdf67ef7edcc33cfacfcee924c06dba67d9c3d1636481ec9bfc1dacc48c2265cb92ba66db5d55695b238e16ab02440576f9306edf93650c2953bafddcf6d6d6d9573df5d55eb89279e18718263ed730eb6af76bf866a072309da0c802ce8efbd4d354f7d5e7fe36d6bebd2c67c19008014643de066bd7d00f5a3fd22099c57c9e31803405fea1beff77dffedb67c14c6e839ffa8e8b215f5d0f37c49f137c56b6c1df2cff3bc9df4de7dc1dd5642cb35d5e517c2307c9d5d17c0e0e2b92c73dae4352a7984684c903c927f8d6a3336e1ca457c95a838e1cac54049802e6cd2a04d6c1a28e1cafdee6e01e8ea5d32a34baa5ab66cd988131ced730eb4af763d12ae00149de6a8872a6eb6e55960be0c00400ab21e70b3de3e80fad17e9104ceabe4718c01605d9ee76da0bef1a52953a68cb375f5f27dff137ace3facd7802b16e9798e50fcbd542abdd6d621b75cc2ddbbaa57ae5aa858a9b8d05dd9ca5de1caae0c60f8e2b92c73dae4352a7984684c903c927fb4997c056d0640163447bd44f1595b9e05e6cb0000a420eb0137ebed03a81fed1749e0bc4a1ec71800d6e5fbfeae2ea1c996d7ababab6b2b3ddfb2465c314bcff305c5638a1d6d1df2a55c2e6f1604c1217aaf7eaa58ae7850f15d95eddbddddbdbe5d1f407de2b92c73dae4913c92af207924ff6833f90ada0c80b44d9e3c7963dff757eb6fc3f1b62e0bcc9701004841d6036ed6db07503fda2f92c079953c8e3100acaba3e792ffd7daf27af9be7fb69eefc7b67ca4f41c9f552ce6b673f9e5ae3aa6f7fb737a9fae57ac098260b69647789ef77abb2e80c620e12a3d248fe42b481ec93fda4cbe823603206dfa7b708ae6a8f36c7956982f03009082ac07dcacb70fa07eb45f2481f32a791c63005897fac5e315dfb5e5f50882604f3dd772cff3b6b17523a1e798ae5842e24eee8cf17d7f1fbd372768b940cb671417e967af542a6d695706d078f15c96396df2481ec957903c927fb4997c056d0640da8220f885e6a85fb0e55961be0c0040836970bdc50db043c42df6714962c0078a8bf68b24705e258f630c00eb0a82e032dff74bb6bc1e7aae5b159fb1e523a17efa30c5e36118ee62eb90be2953a68cd3fb3155e7c8f95a2e553ca49f4fd5fbbcbfe7791bd8f501242b9ecb32a74d1ec923f90a9247f28f3693afa0cd0048d3f4e9d3c76a7efa5c6767e7ab6d5d56982f0300d0601a5cdbaa495583459b7d5c9218f081e2a2fd22099c57c9e31803c0ba7cdf7f200cc3bd6df948a97f0d15f7747777af6feb864b8fef523ce179de1b6c1dd2a3f7e0351d3d5719fbad628d624e100447ea5c9960d70590ae782ecb9c3679248fe42b481ec93fda4cbe823603204dfa5b71b2e6a7b7d9f22c315f060020011d835fe5ea16bb7ed218f081e2a2fd22099c57c9e31803c07fb4b5b56da87ef1a5aeaeae4d6c5d7fdc7fadda3267ead4a95be8799ef27dffbf6cdd70e9f165c5937a8e5d6d1d92a7e3fe761dff6f29ee513c1b04c12fb5f43dcf7b855d174076e2b92c73dae4913c92af207924ff6833f90ada0c8034696efa53fd4d39c3966789f932000009e818fc2a576d76fda431e003c545fb451238af92c73106d0aadca5fd7ddfff7e47cf958bde552a95b60c826077fdfc37bbee40dc6de4b4fe1ccff3de5c5baeb2d355f7b3da324bf51fd0628c2d773a7aae8ef594db1f5b8764e83ddc54c7fc609d133fa91efbbfbaf7b1bdbdfdbddc2a10c8af782ecb9c3679248fe42b481ec93fda4cbe823603202dd57fe47a46b1a3adcb12f365000012d2d1ff55ae6eb1eba581011f282eda2f92c079953c8e31801636467de052f377d073beef2f0f82e07bfaf930fdbc8fbb5a957d604ceb4cad3eeeffdc636a92b65696cbe5f176fd1a6edb4f29ce733fd756689b2aee785acb37d596a3f15cd29ddeaf4fe9785fdbd173abc05b74dcbf1c86e11bedba00f2c9f5c1b54b2487e4917c05c923f9479bc957d06600a4457f637e5073d33b6d79d6982f030090908efeaf72d566d74b03033e505cb45f2481f32a791c6300ad4c7de079fdfc2d541bffa778977d5cccf7fd4f98f5970641f0a09647d8756b699d7d6b1ef3d3eeeeeef55db99ecfab3ec75bec63d0186118eead63fc0dc55d8ae71497ea78075d5d5d5bd97501e49feb476b97480ec923f90a9247f28f3693afa0cd00488be6a5e7ea4fccafd8f2ac315f060020411deb5ee5ea165b9f16067ca0b868bf4802e755f238c6005a99fac0836bfe0eea13beef1f6b1f53cb7d886a1f537ddced8abdecfab1ea15b46a1ff373957d54cb65a552690fbb3eea3779f2e48df55e4cd6b19da97842f1b7eaf16f73b77ab0eb032816d787d62e911c9247f215248fe41f6d265f419b019006773b7acd4b572876b6755963be0c004082dc87cd351ff6b7d9fab430e003c545fb451238af92c73106d0cabababa36513ff842cddf42bd1104c1ecf8ca5303d17adfb58fab89ffa738270cc3adfb79dce27ed67f49f136bb2e464eefddf63a969ff47dff6a2d9fd7efb76a7994e779bbd97501149beb3f6b97480ec923f90a9247f28f3693afa0cd0048437b7bfbfb352fbddb96e701f365000012d651bdca952d4f53d6db07503fda2f92c079953c8e318056a77ef02ad7179a58d1d9d9f96abbaea5f5ceede7b171fc2b0882d33ccfdbd43c66623febc67189fb8fd8daf5313c3ad67bfabeff751dc33f2a56292ed7ef251dcf6decba009a87eb3b6b97480ec923f90a9247f28f3693afa0cd004883fe06fd91e6a5c7d8f23c60be0c0040c234d8b6799e97e980cb800f1417ed1749e0bc4a1ec71840ab533fd85593f0e4e2dfee167476bdfe68dd2bcd63e3b82f0cc377d8f59d20084eee67fddab89ca4aba1b95b05ea184fd2f13a47f1b8e2ef7adfbeafe3fb81e9d3a78fb5eb03684eaedfac5d2239248fe42b481ec93fda4cbe8236032069ee0ad99a932ed3dfa5136c5d1e305f06002079efdb64934ddc80fb3e5b9116067ca0b868bf4802e755f238c6005a9de779db75f4dcfeaf92f01404c1f7ec3a03d1ba73e3c755c3dd16f098b6b6b60dedba31d53f6a1e6363beeffbfbd8c7a1f7bd720972bf563cafb84defc15775bcde64d705d01a5cbf59bb4472481ec957903c927fb4997c056d0640d2341f7d8ffe365d60cbf382f9320000c97249562b1447559799245d31e003c545fb451238af92c73106804a5f785b35d969a9e7791bd9fa8168fdfbab8f73314f8f7d835da796effb7bd5ac5f1b7f54dd974ba5d26bed635a9d8ecb5b8320f89a8ed11ffe7ff6ee04ce8dbafeff780be5b01c1604141045115094c3031104bb82e0afe05650676727db6d57c5fe3c1010410e4156c0a32020a0a81c82dcf721c8557a5041b4a0a51c0544da02a5db72949eb4fe7eff9ff37f7fb39925fd66924db299e43bc9ebf978bc1f33f9ce4cb2bb99e33bb39f4c34fe4667ff5dc5bac78f1fff0e7b5e00adc7ec43f38748ce942953feb37af5ea82220652ffe87d583879f2e435f67b04b75070e55628b8029034f547cf335f736fb7bb82fe320000c9898aada2222bfb71dd70c007d28bed174960bd4a1e7f6300c816f51ca7fde1ff6a78943dad14cdbf40cbfd5f1004e7dad3e268ded372055626cf6af91334dcde9eaf95998237fd3d0fd4dfe67cfd6de62973cdb869aba4180e406b88fab2f469933763c68c858b172f2e286220f5cf0b2fbc70dde4c99367daef11dc4291a23ba14811401d0c577f7481ce5977b627b882fe320000c928565c55ac3d511cf081f462fb451258af92c7df1800b2453e3b6b7fb8d0dc4dc99e564a1004b3b5dc9d767b319a77bab23293c974dad35a99fe8e5be86f3f5e7f9b1b7377b1fa8bb9ab55a5ef0780d643c155fd4c9d3af58bd3a74f5fd6d7d7f71a45248d89feee7df3e7cfbf66f2e4c92f2a87d8ef11dc4291a23ba1481140d274febab7faa34fd8ed2ea1bf0c0040ed0d565435d8f49ae3800fa417db2f92c07a953cfec6002a1186e1fa73e7cebde1e1871ffe9f2953a664bf9aa35573f7dd7787871f7e7878ebadb7164c8b4b34ff95575e5930ad9298bb25987fe0997f7cdbef4f9a789eb78b8e41c72b0f2a4b959b951eb56f69cf0b00c55070555f3a0e1da2cc54fe6d1f9f485d62feeee6ef4fb1550a50a4d8f850a408a05e8220385bfdd153ed7697d05f0600a0b6ca2da62a77be9ae0800fa417db2f92c07a953cfec6002a3177eedc1b67cc98119a4febf38fa37f872b56ac28682b95e5cb9717b4551af377377f7ff30fbc34fde368e2c489eb0541b0bfeffbe7ead8f32f65bef2ab4c26f379be2a1040b528b84225b49e4c635d413d99be5aae488e22c5c68422450075a1fec50b3adffdb0ddee12fa400000d44ea5455495ce5f350ef8407ab1fd2209ac57c9e36f0ca012e6ce567c358a1b31774b30ff40b2df2397789eb7b9effb5d3ad65ca72c51feaac73f0c8260377b5e00a80605572897d69136b39ee4d2664f070000a886ce71f754dfe269bbdd35f4970100a88d6a8ba7aa5dae221cf081f462fb451258af92c7df184025ccd70872672b3762de87c99327afb1dfa346f33c6f671d5b8e0d82e0010d97fabe7fab865fd3e377daf302c0505170857275e6ee6e95cb347b3a0000403574ce3b497d8bd3ed76d7d05f060060e8865a3435d4e507c5011f482fb65f2481f52a79fc8d0154c27c35875df8431a17f37ed8ef51bdb5b5b58dd0b1a42d0882b335fca7f2a272a1effb6364037b7e00a8250aae500e739cca2bb6e22e570000a066d4a7785ee7bfbbdbedaea1bf0c00c0d0d4aa58aa56cf138b033e905e6cbf4802eb55f2f81b03a844b905572b97f685731fb92c7ceafed3b231e3a6cd9e8f0c2d8d2ab8eae9e91915888e21d728af2b3395533299cc1ef6bc0090a4a82f4b9f16a574ae7d772bee720500006ac2f7fd8fa94ff19cddee22facb000054afd64552b57ebe011cf081f462fb451258af92c7df184025ca29b85af1c6cbe193f79e12cefed3716bc5b49969f6fca4fad4b3e02a93c9ece8fbfe31b97f5a2f536e0f82e0f071e3c66d6dcf0b00f512f565e9d3a298cef8bb5b71972b00003064ea4bfc54e7c53fb3db5d447f190080ead8c551c39509b96125ece5ece7ad090ef8407ab1fd2209ac57c9e36f0ca012e5145cbdf4d41f0b8aada22c78ea8e82f949f5295570e579decebeefef64b7974bcbafdbd1d1f1191d27ce0a82e0190d17e8f97eabe1213d3d3d1bdaf3034023447d59fab428a633feee56dce50a00000c99fa12ffcc64321fb7db5d447f190080cad94551a658ea52c51c54cdb0dca2ab62cbd9cf3f641cf081f462fb451258af92c7df184025ca29b87a7adaa48242ab28669a3d3fa93ec50aaecc57fb69ffbe5839d99e568ae7796fd7327e10045769f89af2a872aaf99a047b5e00c8196937d453d497a54f8b389da5ef6ec55dae000040d574debc9bfa1173ed7657d15f0600a03271c5501386f5174d4529a7e82abfd82acaf8bce971af53350ef8407ab1fd2209ac57c9e36f0ca012e5145c3d39b9b7a0d02a8a9966cf4faa4f5cc195f6ebfbf8beff46ee9fc87fb3a7db34ef0e41101cad79a774f67f55e01dca44651b7b5e00b0d4f49a5035a2be2c7d5ac4e92c7d77ab28d3ece500000006a33ec469ca5976bbabe82f030050be6217bce28aa74a155dc5cd7f49ae3d5fb1d7ab18077c20bdd87e9104d6abe4f1370650090aaedc8a5d70d5d1d1f139edd757e6fd13f9ff3ccf7b57fe3ce6ab028320d8d7f7fd499a3e4759a85ca4b67669e89d6a00a44a742de8d8dc70c8d784aa11f565e9d3a25cac2b0000a01672e7d37bd9edaea20f04004079062b7e8a2ba28a2bba8a9b2faed82a32d8eb667577776f64b7e5e3800fa417db2f92c07a953cfec6002a514ec1d553f79f56506815c54cb3e727d527bfe02a088243b54f5f63f6eb56beded5d5b5a9effb9ec6af505ed5f8accefe4fe37e6258f1733c0028c6be06643fae9ba82f4b9f16e5625d01000043e579de2eea53bc382c45e7d3f4810000185cb917b8e28aa9f28baee2a6972ab68a947c7d536ca503fabc200862a71b1cf081f462fb451258af92c7df184025ca29b87aeea15f15145a4531d3ecf949f5890aae748e354efbf3ff35fbf498bcd2d9ff558177f9beffadaeaeae77dbef2b0054a0d8b59f62ed898afab2f469512ed615000030543ab7fe9172aeddee32fa4000009456e985adb8a22af3789d98f6728aad22257f0e536c652ef8172bbae2800fa417db2f92c07a953cfec6002a514ec1d5eb2f3f1e3e79dfa905c556a6cd4cb3e727d5c7bc1f3ab7fab6f6e5ff671559e567f5f8f1e3df61bf9700508592d77c860d3ebde6a2be2c7d5a948b750500000c95effb8f7774747cda6e77197d2000008aabf682565cd1d5b3d6e34a8aad22257f9e5245571cf081f462fb451258af92c7df184025ca29b83299f7e815050557a6cd9e8f0c2d679e79a65d5c159b4c2673b0fd5e0240854a5eebc953ee7c3511f565e9d3a25cac2b000060287cdfdf49fd89978755febfd386a20f040040bca15ec88a2bba8a524db155a4e4cf55ace88a033e905e6cbf4802eb55f2f81b03a8445905576bd684cffde537050557a6cd4c2b989f5495952b578613264c087ddf7f44fbf21b35bc4ac3bf75f67f7da05d74f51bfbbd04800a94bcc613a3d2f9ab16f565e9d3a25cac2b00006028d4973849b9c06e771d7d2000000a8d1cd67f01eb587b4285ccd708da77b6328f4dfb501c1b73a17fd0d84f02201dd87e9104d6abe4f137065089c10aae56bcf172f8cf872e2828b68a62a69979ece54875b9f7de7b63f7e1dab76f1f0441bbf2038d5fa3dc69cf030065aab678aadae52a12f565e9d3a25cac2b00006028d497f8877d338934a00f040040bca15ec02a75872bd39ec81dae22f69dae38e003e9c5f68b24b05e258fbf31804a142db85ab326ec7b6e6af8c43d27151459d931f39879b9dbd5d063de0ffb3d02801a2aebda4e09435d7e50145ca152ac2b0000a05a994ce6fdea4b2ceaeded1dea0d2bea8e3e100000c5557b012baed8cabed35535455715fd3cf945571cf081f462fb451258af92c7df184025e20aae06bbab55b170b7aba187822b0009aae8da4e09b57a9e58145ca152ac2b0000a05ab9bb485f68b7a7017d2000004aabf402565cb1d525c3fabf46d06eafa4e8aad29f232b576c3597033e905e6cbf4802eb55f2f81b03a8445cc1553977b52a16b3acfd7ca4fc5070052021555ddb29a1d6cf3780822b548a75050000544bfd88994110ec6fb7a7017d2000000657ee05ac62c556515155dcf4728aaeca7dfd58ededed2339e003e9c5f68b24b05e258fbf31804ac4155cd9455495c67e3e527e28b8029000fbda8eb916342137ac84bd9cfdbc3541c1152ac5ba020000aa91c964deab7ec42b9ee7ad6b4f4b03fa4000009467b00b5871c554f9c55691b8f94a155d0df6ba65e1800fa417db2f92c07a953cfec6002a115770451a170aae00d4987d6d27ffda50a96b42b662cbd9cf3f64145ca152ac2b0000a01abeef1fa37ec445767b5ad0070200a07cc52e60c51551c5155b45e2e68fbbc056ecf52ac6011f482fb65f2481f52a79fc8d015482822bb742c115801a8abbb63361d8e0d7846c71d792c6e74d8f7b9daa5170854ab1ae0000806aa80ff117dff70fb2dbd3823e1000009589bb80655f282b556c1569c8853200e9c3f68b24b05e258fbf31804a5070e55628b8025023c5aeedc45d132a557415377fdcb5a762af57310aae5029d615000050a9aeaeae77ab0ff15a5b5bdb087b5a5ad0070200a072f605acfc0b5f7117bc8a29b69cfdfc43c6011f482fb65f2481f52a79fc8d015482822bb742c11500c3fce3477dba3b7ddfdfd39e5686c1aeedc41551c5155dc5cd57eadad360af5b160aae5029d61500005029f51f8e0c82e0f7767b9ad0070200a03af6052c73a1cbdca1aad805af62ece5ece7ad090ef8407ab1fd2209ac57c9e36f0cc0d0be609ad266b7db28b8722b145c01303ccf5bd7f4e972b9cbf7fd4fd9f31451eeb59db862aafca2abb8e9a58aad22e5be7e51145ca152ac2b0000a052ea3fccc8643207dbed69421f080080ea0df90296a5d6cf3780033e905e6cbf4802eb55f2f81b0330f2fe515fb2f08a822bb742c11500a3b7b7779dbcfd78947b957dec79f3547a6d27aea8ca3c5e27a6bd9c62ab48a53fc75aa2be2c7d5a948b7505000054c2f3bc77a9ffb044c3f5ed6969421f080080a119d205ac3cb57a9e581cf081f462fb451258af92c7df188011f38ffad8c22b0aaedc0a0557007286c7ecc7b3098260b286fb59f3577b6d27aee8ea59eb7125c556916a7f1e0aae5031d61500005009f5a7bfadfec315767bdad007020060e8aabe809533d4e507c5011f482fb65f2481f52a79fc8d0118f63fe8f3b256e11505576e85822b009198fdb79d29411098eb3943bdb613577415a59a62ab48553f97f9ddf287c060585700004025d4879eaafec358bb3d6de8030100501b555dc01a56fd7215e1800fa417db2f92c07a953cfec6008c987fccdb99a6b45170e556720557c3cdd789799eb76e5b5bdb0893891327ae67beee60cc98311bf4f4f46ca8f1b7b5b7b78feceeeede48e31b8f1d3b7693aeaeae4d35fe764d1f95c96436d3f8e6e3c78f7f4710045b687c4bcdbb95c6df6962be4261dcb8715b6b1dd846e3db6ad9776b7c3b8dbf47cbbe57e3db6bfc7d1a7fbfeffb3b68fc031adf51e33b697c673dc70735fe21133dde458f3facf18fe87976d5f86e1adf5df3efa1e7f9a8c63fa6f18f6bfc131adf53e39fd4f85e1aff94e6dd5be3fb7474747cda448ff7d5e3fd34fe19534862d6518d7f56e3fb6bfc008d7f4ee3076ad983f43c9fd7f87f697c8cc60fd6f44334fe05b5b56b7cac891e7f518f0fd5f8611aff92c6bfacf1af68dcd37887c635ea776a3cd07846e35d1a1da7f16e8d8fd7f8048df798e8f15735fc9af275b51faee13794891aff6f0dbfa9e9df0afa3f29fd1d8d1fa1f1ef6afc488d1fa5f1a335fe3d8d1f63a2c7dfd7e363357e9cc67fa0f1e3357e82c64fd4f8491affa186272ba768fc471a9eaaf46afa8f353c4d395de3679868fc27ca4f35fe330d7faef92769fc4c8d9fa5f15f68fc6c8d9fa3f17335fe4b8d9fa7f1f335bc40f995c67fade185ca6f4cf4f8b71afe4eb948e3176b788972a996fdbd869729976bfc0f1a5ea15ca9f1ab34bc5ab946e3d76a789d72bd891edfa0e18d7a9e9b34bc59b945e3b76a789b72bbc6ffa8e11dca9d1aff93867729776bfc9ecefeafcfbb4fcf31d944e3f777f61718997f8e98fdd7748d3fa0e10ce5cfca83ca43ca5f948795bf2a7f53662a8f288f2a7f57fe61a2d798a5e163ca6c8d3faee113ca937acea7349ca33cadf167347c56f9a7f29cf22fe57965ae324f999fcb0bca8bca4bca02e56565a1d2a72c52162baf28af2aaf29af2b4bf4ba6f68b85459a6d75a6ea2f115ca4a655567e17ebb20dafec20d37dcd0ec378eb58f0315325f2368dfd9ca3c36ed4371acfd33139244ec150f0000208e393736fd70735e6d4f4b1bfa400000d44ea5c55395ce5f350ef8407ab1fd2209ac57c9b3fff9400821a542c1955bc9bd2fff51fe4ff97fcaffe6f23fcabf9535ca6ae5cdcefe820c5398b12257a8b14c599a2be258d2d95fd4618a3b4c918729f630451fa6f8c3c414829882105318620a444ca1882918318523a680645e677f5189292e314526a6d8c4149d3c9b2b4279bab3bf28654eae40e549e5895ce1ca6ce5b15c418b296e31452ea6d8c514bd98e2175304638a614c518c298e314532a658c6c414ceccc815d24c57a6e50a6ca628f7e70a6fee53eecd15e5dcaddc952bd6b953b92357c4737b2eb7e50a7c6e516ef6fb0b7f6ecc150299a2a0eb82fe22a16b94ab83fee2a12b952b82fea2a2cb3bfb8b8c2e0bfa0b8e2e552ef1fb0b912e527ee7f717289962a50bfdfee2a55f2917f8fd454de769b95f6afc5c8d9fa3f1b34df4f8177a7c96c6cfd4f8248dff3ce82f98faa9f293a0bf90ea74e5b4a0bfc8aa5739d5ef2fbe3a453959e33f34d1f8499ae7448d9fa0f1e335fe038d1fa7f16335fe7d8d1fa3f1ef69fc688d1fa5f12335fe5d8d1fa1f1ef68fcdb1aff96c6bf6912f417904d54be11f417967d5df99adf5f70d6a3b6091a1faff16e8d8fd37897c6331a0f34aed14e5fa31d267aece9f15734fe658d7f49e38769fc508d7f51e36335deaef12f68fc105334a7f1316afb2f534ca7f183347ea029b233d13c07e8f1fea6004fe36d1a1f6d0af334be9fc6f735057b1adf47e37b6bd94f697c2f53dca7f13d35fe0953f4a7f18f69fca326a620508f77d7fcbb9942418d7f44e31f3605841aff90c63f688a0b35be9329363445871adfc114219a62443dc7f6a638d1c4142aeaf176a670d114306a7c1b53d0680a1bf53cef34c58ee61f3b1adfc214416a7c732db799298ed4f8db4db1a4299a34d1e38d4d21a529a8ececdf0f14ecb7f5733ca8650e18f6d69da7867a8da7d41dae4c7b43ef70150d81625847000040b93afbcf75aeb1dbd3883e100000b555ee85ac72e7ab090ef8407ab1fd2209ac5700501ff63fe8f332ad93af1474367ca5208048677fd165fefefb21538466cf9753edb59eb8622bfb4e57d5145d55fbf314145a71fe80c1b08e00008072a9df709ff93088dd9e46f4810000a8bdc12e680d36bde638e003e9c5f68b24b05e01407d58ffa42f28b48a5070e55628b80210e97cabe0eaafe6ae5ff6f418955ef3892bb6ba6458ffd708daed95145d55fa73ac253a5fb0874031ac230000a01ce66eb3ea372cf53cef6df6b434a20f040040328a5dd82ad69e280ef8407ab1fd2209ac5700501f83155a455c2ab83afffcf3b3ffd8df64934dc2a54b97164c8fcbf4e9d3c3abafbe7ae0f182050bc251a3468593264d2a98370da1e00a4044fbeebf98af59b4db0751eeb59f62c556515155dcf4728aaeca7dfda2ec422bce1f30b177e779000080004944415418d6110000500ef519be1604c10d767b5ad10702002039f6052efb71dd70c007d28bed174960bd0280fa18acd02ae252c1d57efbed176ebffdf6d97fec5f7bedb505d3e372f0c10787871d76d85a6dd3a64d0bfbfafa0ae64d4328b8025003835d038a2ba6ca2fb68ac4cd57aae86ab0d72d8b5d68c5f90306c33a020000caa13ec35d411074d8ed69451f080080644517ba8ecd0d8774c1ab5a1cf081f462fb451258af00c02dae145ccd9b372f1c3e7c7878fae9a787db6db75d78e8a187ae35fdd24b2f0d77d8618770830d360877dc71c7f099679e094f3ae9a4fc2280f0b2cb2e0be7cc99931d3ff9e493c3dd77df3dfb5c6bd6acc93e477777773862c488f0a5975e0a2fbae8a26c71d7c89123c37df7dd379c356b56c1cfd488507005a046a26b42f6b5a0b822aab862ab48dcfc714557c55eaf6276a115e70f180ceb080000184c4f4fcf28f519967677776f644f4b2bfa40000024af6617bcaac5011f482fb65f2481f50a00dce24ac1d599679e99fd47fecc9933c36f7ce31bd9c2aa575f7d353bed91471e09d759679d70f4e8d1d9af0feceded0d57af5e1d3ef0c003e17aebad17eeb3cf3ee1adb7de9a2ddaca2fb83ae79c73b2e366be152b5664bf6ab0bdbd3dfb7ca6b8cb14755d78e185e1aebbee1aeeb1c71e053f532342c115801a8abb26346158f9c55691b8a2abf179d3e35ea76a76a115e70f180ceb080000188ceffbe3d567b8c56e4f33fa400000d4c748bba19e38e003e9c5f68b24b05e01805b5c29b8faf8c73f1e6eb1c516e18b2fbe185e72c925d97fe89ba1991615634d9d3ab56039539895ff9582f905570b172e0cd75f7ffdf088238e086fb9e5966cfbcd37df3cf07cf931055dab56ad2a78fe7aa754c1557b7bfb48dff70fd2b1f4ac2008ceb6a703408cd1c3d62e86ca2f9e2aa7d82a526c39fbf987cc2eb4e2fc0183611d01000083517fe10e2563b7a7197d2000005a00077c20bdd87e9104d62b00708b0b05574f3ffd74b6e8c9ce81071e989dfe8b5ffc22fb78dab46905cb962ab8328fcdb46db7dd369c306142b8d5565b658baa264d9a949de7fcf3cfcf16714589be7ab091b10aae86fbbebf67100427eaf8394559638ea3b99c9e371f0094327a5861d195b94355b9c556117b39fb796bc22eb4e2fc0183611d010000a57475756daafec2b2b163c76e624f4b33fa400000b4000ef8407ab1fd2209ac5700e016170aaecc5704ea47097ffef39f8777dc714736fbedb75fb8eebaeb860b162c081f7cf0c1ecf4b6b6b6f09a6bae094f38e184ec9db0ccb2e6ae583becb04378fdf5d787fff8c73f0a0aae6ebffdf6ece32db7dc323ce69863b26d8f3efa68f68e567befbd77f62b0acdd70a9aa22efbe76a446ebae926534c353108821b345c9257606567ecdaef24009454ebe2a85a3fdf80e87cc11e02c5b08e000080527cdfef52fe68b7a71d7d2000005a00077c20bdd87e9104d62b00708b0b0557bbecb24b386ad4a870e5ca95036dd1d70a9e77de79d9c7e79e7b6ef89ef7bc277b47abdd76db2decebebcbb69bbb546dbef9e6e1a69b6e9a2dc6b20baede7cf3cd70ebadb7ceb6cd9e3d7be0f9afb8e28a70a79d760ad75b6fbdec9dafce38e38c829fabde313febb871e34c31d5ffc51458ad15cff3b6b5de4a00184cad8aa46af53cb1ec422bce1f904febc334fb98189369f6720000a075f9be7fab62eed4da54e8270300d00238e003e9c5f68b24b05e01805b5c28b8226fe59e7bee09832098d059faee568b7b7b7bd7b1df4b0028c3508ba586bafca0a2f3057b08185a1fda628e8b76daece50000406bf23c6f63f50d96f5f4f48cb2a7a51dfd6400005a00077c20bdd87e9104d62b00700b05576ec5bc1fe67d0982600b6572cc3f914d56e4f267e51c25e3fbfe4e5a6cb8f5f602409c6a8ba6aa5dae22d1f9823d04229da5ef7235cd9e1f0000b42ef50d7ce52ebbbd19d04f0600a00570c007d28bed174960bd0200b75070e556a2822bc3f3bc758320f8998e9dffb1fe997c8aa6bd5dd3f6577ea0dca0b679ca52658aeffb9334fc8ab27dde5b0d00f92a2d9eaa74feaa45e70bf610887496becb559b3d3f0000685dea1bdca87ccd6e6f06f493010068011cf081f462fb451258af00c02d145cb995fc82ab888e9d8729cba27f26fbbe3fc69ec7183f7efc3b3299cce735cfc9ca6dca02e555e56ee5742df7450db7b19703d0b2ca2da22a77be9a88ce17ec2190af33fe2e57d3ecf9000040eb6a6f6f1fa9fec152cff336b7a73503fac90000b4000ef8407ab1fd2209ac5700e0160aaedc4a5cc195e179dece41103c658ea31adfd29e5eccb871e3b6d672edca8fb5ec5dca2bca42dff7ffa8e129a678ab92e703d074062ba61a6c7acd45e70bf610c8d7197f97ab367b3e0000d0ba741efc65f50feeb3db9b05fd6400005a00077c20bdd87e9104d62b00700b05576ea558c195e179dec641109c6db7574acff31e73e1d97c5da13259c7e625ca7cdff76f524ed0f8013d3d3da3ece50034ad624555c5da13159d2fd843c0d6b9f65daea6d9d30100406bd3f9eeb5ea234cb4db9b05fd6400005a00077c20bdd87e9104d62b00700b05576ea554c155923ccffb80effb9da6a04b794059ae63f673ca356a3fa6a3a3e333a6e0cb5e0e40d3b08babecc77563175a71fe80623ad7becb559b3d1d0000b4ae9e9e9e0d752efb4633dfd1997e3200002d80033e905e6cbf4802eb1500b885822bb7d2a8822b5b6f6fef3a9ee7ede2fbfe78e57c1dbf1f56569aaf3554fea0b623944f998bd8f6b200522b2ab23a3637ac7bb19561175a71fe80523a7377b9b2db0100406bd3f9ea1775ee3ad56e6f26f48100006801ae1ef0c3305c7feedcb9373cfcf0c3ff3365ca94ec3f36487da3bffb7f66cc98b170ead4a95fb4df1fb8c1d5ed17e9c67a05006e31fd32bbe887342ee6fdb0df2357b4b5b58df07d7ff720080ed7f0b73aa63faaacd2f82ce562b5ffb7861f9b3871e27af6b20052232aba6a48b19561175a71fe80523a7377b9b2db0100406b53ffe04a9da37edb6e6f26f48100006801ae1ef0e7ce9d7be38c1933c2c58b1787ab57af2ef84707493ee6ef6efefed3a74f5f3679f2e443ecf7088de7eaf68b7463bd0200b75070e5565c2eb88a3366cc980d7cdfdf53f9561004bfd7f0715384a5ccd4f8af95af767575edea79debaf6b2009c35d26ea827bbd08af387e4f061c4c6870f23a60bdb4ce3c33603a01c3aff5c5f7dc8251abecb9ed64ce8270300d0025c3de09b136353ec63ff8383d43f7d7d7dafe98479a6fd1ea1f15cdd7e916eac5700e016f38f0bbb7f461a17f37ed8ef51dab4b7b78fece8e8f8b4effb47e53e55fc8c862b940735fe4bb577799eb7b3661d6e2f0b0076a115e70fc9e1c3888d0f1f464c17b699c6876d064039d47f3c44e79e0fd8edcd867e3200002dc0d503bef9141227c66ec4bc0f3a415e63bf47683c57b75fa41beb1500b885822bb7d20c055771bababa36551fa0cdf7fde334bc5e795e591a04c154e54ca5c3f3bcf7d9cb01683d76a115e70fc9e1c388ee840f23a603db8c3b619b01508afa8f97e91cf3bb767bb3a19f0c00400b70f580cf3f96dc4ab3fe6329ed5cdd7e916eac5700e016fac56ea595fac59ee76deefbfe41ea1b9ca4dca2bca4bca6dc1b04c119caa15d5d5defb69703d0dcec422bce1f92c38711dd091f464c07b61977c23603a098891327ae67ce2b75beb9ad3dadd9d04f0600a005b87ac02ff71f4b2b97f685731fb92c7ceafed3b231e3a6cd9e8f0c2dadf48fa5347175fb45bab15e01805bcaed1793faa4d5fbc54110bc537d8543945ee54e6591d2a7dca19c9ac9640eeeeeeedeca5e0e40f3b00bad387f480e7d00b7d2ea7d8034609b712b6c3300e2e89cf1f3ea3f3e64b73723fac90000b400570ff8e59c20af78e3e5f0c97b4f0967ffe9b8b562dacc347b7e527d38417693abdb2fd28df50a00dc524ebf98d42ff48b0ba9efb09d7298f213e53ee575e545e5e620084e540ecc64329bd9cb014827bbd08af387e454d207e85bb2323ce6dac7b231e3f67432f4d007701fdb8c5b619b0110c7f7fd8bd57ffc9eddde8ce8270300d0025c3de0977382fcd2537f2c28b68ab2e0a93b0ae627d587136437b9bafd22dd58af00c02de5f48b49fd42bfb83c994ce6fdea53f8ca59ca346599f22fe5ba2008beaf8c1e3b76ec26f67200dc67175a71fe909c527d80356bfe1dbebefccd81c7339e5e34503c62c6a3f65796ae0a97ae786b3e527de803b88f6dc6adb0cd00b0799eb7aefa8eaf68f81e7b5a33a29f0c00400b70f5805fea0439cad3d32615145a4531d3ecf949f5e104d94dae6ebf4837d62b0070cb942953feb37af5ea82fe19a97ff43e2c54bf788dfd1ea12cc38320f8a0324e7d8df394879415cad3ca15ca91ca3e9ee7bdcd5e10805bec422bce1f9253ecda98291cb9eaa179e189373e1edef7c4c270fa9cbef0945b9e1c281e31e3a6fd4fb3168427689e73ee79365cb6920292a1866b63ee639b712b6c33006cea371ea0fccd6e6f56f49301006801ae1ef08b9d20e7e7c9c9bd05855651cc347b7e527d38412e6aa4dd504fae6ebf4837d62b0070cb8c1933162e5ebcb8a07f46ea9f175e78e13af58b67daef11aa633edd1c04c16eea7b7c4db950794459a5cc562e55bea97c42f3ad6f2f0ba071ec422bce1f9253ecdad86bcb5685c7dff0f840b1c86039f6bad9e14baf2d2f781e5259b836e63eb619b7c23603c0a67ee36f7cdf3fce6e6f56f49301006801ae1ef08b9d20e78782abfa8513e458a395c5b96143b8bafd22dd58af00c02d53a74efde2f4e9d397f5f5f5bdd62c77ba4adbefa19fb76ffefcf9d7a84ffca27288fd1ea1764c71552693f978aed8ea12e5b15c119629c6fa8df27553a4d5d6d636c25e16407dd885569c3f24a7d4b5b1bb1e7bb9a048a4586ef8db0b05cb93cac3b531f7b1cdb815b61900f97a7b7bd751bf7191f9fa797b5ab3a29f0c00400b70f5805fea0439ca53f79f56506815c54cb3e727d58713e40251b1d5b1b961438aae5cdd7e916eac5700e01e53e463eeaca4fcdbf4cbd2962baeb82234c717337edd75d7855d5d5de1bdf7de5b309fc3317f77f3f7a7d8aa01ccd70c0641b0b7f2ddcefeaf1f9ca3ac54fed2d9fff584ddbeef7fc85cc0b79705507b76a115e70fc931c720fbfa4c14f395685171c871d7cf0eaff9cbfcf0f9454bb331e3a62d9a7ef7ec970b962795c7bc1ff67b04b7b0cdb815b61900f9743e375afdc6bfdbedcd8c7e3200002dc0d5037ea913e428cf3df4ab8242ab28669a3d3fa93e9c20af252ab68a8aacecc775e3eaf68b7463bd0200b7f5f4f48cd2befa10e52741104ccd159e640b9a72792593c9ec612fd748fa99a645c717fdcc676afcff7df9cb5fdecd9e0f28d7d8b163373117ec95ef2bd76a9dfa97b24c99eefbfe2f34d4c0dfc15e0ec0d045fb737b88da8bbb36d6b7646538e3e945e129b73c39501c628a45ecf94c5b34ddcc6b9631cbdaf391f2c3b531f7b1cdb815b61900f9d467bc40e76e27daedcd8c7e3200002dc0d5037edc09b29dd75f7e3c7cf2be530b8aad4c9b9966cf4faa0f27c8038a1557156b4f94abdb2fd28df50a00dce4fbfe31da473f69f6d3c51204c1ea71e3c66d6d2fdb48fab9daa29f2f93c97c56c305b99ff51e7b5e6028b47e6dd6d1d1f13973115febd8cdca0bca12e53ee5a7da86bea4e176f672002a63f6e17143d45edcb5b1a820243fe60e3df67ca6cd9ecfc49e8f941fae8db98f6dc6adb0cd00c8335c7dc69775ceb6a33da199d14f0600a005b87ac08f3b418ecbbc47af2828b8326df67c6468e104396bb0a2aac1a6d79cabdb2fd28df50a00dc64eed6a37df4eb663f5d2ce66e3ff6728dd699bbbb552e8fe5ffbc994ce6607b7ea096babbbbb732eb99b69f1f699dbb43e95316ebf19f34ecd5f00b9ee7bdcb5e0e407166ff1d3744edc55d1bb38b41281ea95fb836e63eb619b7c2360320d2d1d1f169f51967dbedcd8e7e3200002dc0d5037edc097241d6ac099ffbcb6f0a0aae4c9b9956303fa93a9c20975d4c55ee7c35e1eaf68b7463bd0200770541b0bff6d3ff9b5fb49497a5e66bd6ec651aa933efee5645f2525757d7a6f67240923ccfdb56dbd2a1ca19beefdfa3f5f035b32e6afc56e587ca419a67737b3900fdccfe3b6e88da8bbb36c6d7a3352e5c1b731fdb8c5b619b0110d139d6b9ea339e62b7373bfac90000b400570ff87127c8f959f1c6cbe13f1fbaa0a0d82a8a9966e6b19723d5a5c54f902b2da2aa74feaab9bafd22dd58af00c06dbeef1f1153b86472963d6fa375ae7d77ab62f99dbd1c506f9ee7bd4fdb961704c199ca54ad974b95b9caf56a3faea3a3e3b3140702fdccbe3b6e88da2b756d6cfa9c4503c521c75d3f3b5b2c62eed06362c64d5b34ddcc6b2f4f2a4f8b5f1b4b05b619b7c2360320c77c9de08b3aaffa903da1d9d14f0600a005b87ac02f7a82bc664dd8f7dcd4f0897b4e2a28b2b263e631f372b7aba1a7854f90ab2d9eaa76b98ab8bafd22dd58af00c06d6d6d6d23b4af7ed22a5afa5fcff3de63cfdb489d83dfdd2aca7fcc9dbbece581061bae6d6a67dff7bb729fc67e5059a175f519e52ab51d65be16a3bdbd7da4bd20d0ecccbe3b6e88da2b7a6d4cb9e7f197078a4306cb9db316142c4f2a4f0b5f1b4b0db619b7c23603c0505f712f9d433d65b7b702fac90000b400570ff87127c883ddd5aa58b8dbd5d0d3a227c8432d9a1aeaf2837275fb45bab15e0180bbccd79c693f3d2508823fe50a40b2454b7a7cad3d6fa375967777ab28cf7777776f643f07e0126d7febfabeff11e5abcaafb5defe4d59a5f1c7b50dfe5ef9b6c6f71c3366cc06f6b2403331fbedb8216a2feeda98c96bcb568527def878419148b1fce086d9e12b4b57153c0fa92c2d7a6d2c55d866dc0adb0c00437dc5b374aef463bbbd15d04f0600a005b87ac08f3b412ee7ae56c56296b59f8f949f163c41ae55b154ad9e2796abdb2fd28df50a00dce479de2eda47ffcbf7fd49bdbdbdebe8f1967a3ccfecb74d91873d7f2375967f77ab810441f04bfb7900d74d9c38713d6d7f1fd33a3c51c38b95591a5fa5fc5de3bfd57a7d782693d9c3dc99ce5e16482bb3cf8e1ba2f6e2ae8d99ac7a734df89ba9cf65bf02edd6475f0ca7cd59149e72cb9303c522667cfa9cbef0be2716668b4c7e3fe3f97035777f1f725af0da58eab0cdb815b6190086fa8af3bababa76b5db5b01fd6400005a80ab07fcb81364bb88aad2d8cf47ca4f8b9d20d7ba48aad6cf37c0d5ed17e9c67a0500eef17dff0bda3f2f0e82605c7ebb1eefa6f6bbf3db5cd059feddadfe6aeecea59c69ee0e64ee20643f1790363d3d3d1b6a9bfd947284d6f1cbcdd76768b8527958b940ede34d01a5299cb49705d2c0ecbfe386a8bdb86b63514c01c9a237560e3c9ef1f4a281e211331eb52f59fe268523354a8b5d1b4b25b619b7c2360340fdc44f28cfdaedad827e3200002dc0d5037ea9136452ffb4d009b25d1c355c99901b56c25ece7ede9a7075fb45bab15e01805b7cdf3f41fbe6973299cc27ed69461abebe4cbf83678e2f41108cd6707b350dd7708e293ab1e7059a91d6f58db5ceefa76de1180daf51fea9ed61b9f28072b612689e0f0cabfcbc03a83bbbd08af387e454726dac6fc9ca81e211336e4f27434f0b5d1b4b2db619b7c2360340fdc49f2b3fb1db5b05fd6400005a80ab07fc4a4e9049f269911364bb28cafcb3e352c5fcee6658ee3f3f8a2d673fff90b9bafd22dd58af00c00de62e39da275fadcc54b6b1a7a78d7d7cf17dff9e2008fe2bbf0d6825dac64769bb3840395edbc34d1aced7f00d0defd7b6f133e5cb994ce6bdf67240a3d98556f6fe1db5c3b531b7d222d7c6528d6dc6adb0cd00503ff139e5a3767baba09f0c00400b70f580cf09b25b698113e4b862a809c3fa8ba6a2945374955f6c15657cdef4b8d7a99aabdb2fd28df50a001acff3bc6d4da15510045799c22b7b7a1ad9c717dff72f56dbc4fc36a0d569dbdf52dbc6186d1ba728b72b0b955794bb94d3b44f681f376edcd6f672403d45fb737b88dae3da985b69816b63a9c736e356d86680d696c964f6503ff179bbbd95d04f0600a005b87ac0e704d9ad34f90972b122a8b8e2a952455771f35f926bcf57ecf52ae6eaf68b7463bd0280c6d27e782f658172bc3d2dcdece38b1e9f1204c119f96d000a695bd94619ab9caedcadbcaabcacdca69c6cee14a76c612f072425da9fdb43d41ed7c6dc4a935f1b6b0a6c336e856d06686de67cdff7fd49767b2ba19f0c00400ba8f7015faf374d69b3db6d9c20bb95c14e90cd7b6ade5bbb3d69dddddd1bd96d151aacf829ae882aaee82a6ebeb862abc860af5b967a6fbf680dac5700d038da07772b8b7ddfff823d2dedece34b100413d476457e1b80f268dbd95ef98af273654aeeab08e729376adbfa81b2bfe7796fb797036a21da9fdb43d41ed7c6dcca60d7c6d0786c336e856d06686d3a277946e7297bdaedad847e3200002da0de077cf37ab9942cbce204d9ad143b4136ef61eebdccbeaff6f42499622bf34f0575dcab2d5a2ab7e829ae982abfe82a6e7aa962ab48b9af5f54bdffe6680dac5700507fbdbdbdeba84f73a6f6c1cf799eb78b3dbd19d8c7978e8e8ecfaa6d7a7e1b80aa0dcf64323b6a9bca28e72833b44f59aee1b3cad5caf7f478df1a7c60052828b4b2f7efa81dae8db99562d7c690bcce41ae2347d866dc0adb0cd0ba7cdfff88f6dbf3edf656433f1900801650ef03be793d2bb127cc9c20bb15fb04d9bc67b9f76eadf7337f9e7a30c5567add57aa28baaab4d829aea8ca3c5e27a6bd9c62ab48a53fc75a1af13747f363bd0280faeaeaeadad4f7fd3f69ff7bbfe7799bdbd39b857d7cd1efbc83da9ecf6f03503bb942ce0fe7ee26f72be5afca4ae549e532e53bca5e3d3d3d1bdacb02a544fb737b88dae3da985bb1af8da17ecc7e2697d8ebc811b619b7c23603b42eedab7b751e72b6ddde6ae8270300d002ea7dc0cf3b41b6b3d6093327c86e253a4136ef51eebdb2dfbf6c06dee83aaaa2e8aada22a7b8a2ab67adc795145b45aafd79eabefda235b05e0140fde4ee48f3b4effbe7b7b5b58db0a73713fbf83266cc980dd4b66658e57d2700559a3871e27adaee3eaa7c43f99df20f65556e7851aefda3663e7b592012edcfed216a8f6b636e85e291c631fb192b6b5d478eb0cdb815b619a075691ffd6410047bdbedad867e3200002da0de07fc9813643bd913664e90ddca15575c11bd37f6fbb556ecf7bb5e2a28baaabab82927aee82a4a35c55691aa7eae46fecdd1bc58af00a03ed46f3950fbdc45ca37ec69cd28eef8627eff71e3c66d6db703a81f53fc98c9643ed9d97fc7abcb94273afbef8465ee8865ee8cd563ee9465ee98652f8bd614edcfed216a8f6b636e85e291c631fb992231d72adba2f9d866dc0adb0cd09a74eef041ed9b5f1a56fdff4a9a06fd6400005a40cc89aa13e104d9add8ef4f9aa3d5fe587b3ba890f947837d672bf378a8ff8038d6fe59cb89fd24c050b15e0140f27cdf3f4afbdb85ca7ef6b46615777c51db4c652fbb1d40637577776f1404c1becad1da46af569e5556287f56ce5132da8fed348c7fa2b4a4687f6e0f517b5c1b732b148f348ed9cf0c92699d7c80d7b9b0cd00ad49fbe39395f3ecf656443f190000d45ccc09f15a27c6d17c9c20bb1597bf52305f1977ba32ed15df492a4fa93b5c99f66affe150d5cfe5c2df1ccd87f50a0092e379defadacf5ea23c96c964de6b4f6f6671c717dff76f523cbb1d807bb4ff7abbceb3f6577ea0dca06d7a9eb25499a2ed7892865f51b6b79743f389f6e7f610b5c7b531b7125dfb22ee866dc6ad507005b426ed8f1feb6ca10f9795628e4d761b0000c090d827c29d56a155841364b7629f209bf72cf7deadf57ee6cfd32809165dc5155bd977baaaa6e8aada9f870e3b12c17a0500c9e8eeeede4afbd807959bcddd63ece9cd2eeef8a2b673d467fbbedd0e201dc68f1fff8e4c26f3f9cefe4fb1dfa62c505e55ee564ef77dff8b1a6e632f87748bf6e7f610b5c7b531b7625f1b43fd98fd4c91ac755d996dc6adb0cd00adc7f3bc0f68bfbc90af23ef473f190000d45cb113625ba913e4e5cb9787c71c734cb8cd36db841b6eb861b8cb2ebb8437dc7043c17c43cdf4e9d3c3abafbebaa0bd593367ce9c6cd1d0c9279f5c30add809b2790f73efa533055746aee86a6e7b7bfb487b5a4ea5454e71c556970cebff1a41bbbd92a2ab4a7f8eb5b8f43747f360bd0280dacb64327b68ff3a5fe91d567e3fa1a9c41d5f725f57c6570d004d64dcb8715b6bdb6e577eacedfb2ee51565a1effb7fd4f0140dc7789eb7a5bd1cd223da9fdb43d45ea96b63a4fe29766d0cc98bae3be625f6ba32db8c5b619b015a8ffafa2728bfb6db5b15fd6400005073c54e886da54e904d2765f8f0e1e151471d15fee10f7f08bffad5af867ffef39f0be61b6a0e3ef8e0f0b0c30e2b686fd65453701531efa9796fedf6462a516c1529b7d8a958b155f4cfd2b8e9e5145d95fbfa45d161471258af00a0b63afbbf66cb141c7cc59ed64ae28e2fbeef7f49edb7d8ed009a8be779ef0982e0cbcacf94c9daee9728f3735f2b7a82c60fe8e9e919652f073745fb737b88da2b756dacd214fbf062a96b41d5a492e7abe4838e953c6f5219ecda189263f633b994bcae5cab6d66d5aa5503d7f82ebef8e281f6a38f3e3adc68a38d0ae68f8bbd7e2f58b0201c356a543869d2a4ece34b2eb924bb3d6eb0c106e1b7bffdedb5a65593c15eaf11619b015a8ff6d18f7674747cd66e6f55f493010040c3143b417eeeb9e7b227bbeab4154c8b2ebe5c70c105e14e3bed147efef39fcfb65f74d145e1f6db6f1f8e1c3932dc77df7dc359b3660d2c638ab6a28b4d7beeb967f8f7bfff3ddb7ed249270d9c589b5c76d965833e577e8a3d6ffe05a21d76d821dc6cb3cdc2534f3db5ec69f6ef76cd35d7843befbc73f6753ef1894f840f3df450b67df7dd770fb7db6ebb70cd9a35d9c7dddddde1881123c2975e7aa9ac9fcdfe7d9af80479f4b0d2454f71c554f9c55691b8f94a155d0df6ba65a1c38e24b05e0140cd0ccfdde165beb9c3953db1d5c41d5f7cdfdfd35c90b5db01343ff37523da07746a3f79b6f280b25cfb83e7946bd47e8ccef93fa37936b69743e3d9855671fb77d446b16b63d5c4bc4f711f5e8cae05fde8473f2a58a69a94bab664a7920f3a56f2bc49a589af8d396fb042ab48adb699fc82ab77bdeb5de192254bb2ed95145cc5addfd3a64d0bfbfafab2e33beeb863f6faacb9b6fbe4934fae35ad9a0cf67a8d08db0cd05ad4777f9ff6d58b355cd79ed6aae827030080862976827ce38d37664f76cf3befbc8269d1c51773e2fbb39ffd2c7cf0c107c3471e79247b41e9d0430f0d2fbcf0c270d75d770df7d8638f81656ebbedb6f077bffb5df6f936d9649370cc9831d9f6071e78205c6fbdf5c27df6d927bcf5d65bc379f3e60dfa5cf929f6bcd1cf688aa1cc05ae030e3820fbf8af7ffd6b59d3f27fb7c71e7b2c5b4475d0410785575e7965f8c10f7e30dc628b2dc237de78233ce79c73b2f39bdf63c58a15d94f34b5b7b797f5b3c55dbc6af213e4d1c3e28b9fe28aa8e28aad2271f3c7155d157bbd8ad161471258af0060e8babbbb37d2fef466e5cf1adfca9ede8ae28e2f4110bc53edafd8ed005a4f6f6fef3a9ee7ede2fbfe78e57ced1b1e56566a3ff194f207b51da17caaa7a767437b59d4975d6815b77f476d14bb365669caf9f0a2f9a0de6ebbed166ebcf1c661269309df7cf3cdecf4521f3cbcf4d24bb31f183477e831c523cf3cf3cc5ad796cc8700c78d1b97fdc0dffdf7dfbfd6eb16fba063b10f1696fbbc49a6c9af8d35855a6d3351c19529603243b3be9a76bbe0aad8fa1ab77ee7afc3471c7144f61a7334fde69b6f1e9866968fdbb64c7bb10fd10ef67aa57ed668be9ffffce7e1873ffce1ec3e40c7dd817dc050c23603b416f5098f557ffdb7767b2ba39f0c00001aa6d809b2b9ddb92667eff4644f8b4e10274e9c38d076e69967ae75c269b2ce3aeb644f9c172f5e1c7eee739fcb162d45d3cc9da1a265cd496dfe27834a3d57fecf51ea79a39fd1dcc6dd3cbef7de7b077e9f72a6e5ff6ee79e7b6eb6cddc32da3c360554e6b1596ee1c285e1faebaf9f3d81bfe5965bb2ede6e4bd9c9fad050bae8cd1c30a8ba0260ccb7baf87952eb68ac4155d8dcf9b1ef73a55a3c38e24b05e01c0d0643299f76a5f3adbf7fd8b3dcf5bdf9edeaa8a1c5f86ab7db5fe4e6fb32700405b5bdb08ed4b770f82e070f3cf1bed2f1e5556697c96d9c7aafdbf35fc98ce93d7b3974572ec42ab22fb77d440b16b6395a69c0f2fbee31defc87e755a5460623eac57ea8387669ab926367af4e8ecd798f5f6f686ab57af1e783e73c7ac238f3c325c77dd75c39b6ebaa9e075e33ee858ea8385e53e6f9269816b63a957ab6d262ab83afbecb3b3df32f0b6b7bd2d7cfef9e7c3ef7def7b030557a5d6d7b8f53bffbaabb9f3d4965b6e197ee8431fca4e9f3973e6c0b462db9679cd621fa21decf54afdacd17ce6b1f99ac32f7de94b03fb00fbef5269d86680d6a23ee15f3b3a3a3e67b7b732fac90000a0618a9d203ffef8e3d993be9e9e9e8269710543e67bea4ddbf9e79f1f4e9d3a7520e6d370510195f97492793df3893df369be6859bbe0aad473e5ff1ca59e37fa19cd2792cce37beeb927fbf857bffa5559d3f27fb7b3ce3a2bdb664ed2cde35ffef2976b3d363ffbb6db6e1b4e983021dc6aabadb2170bcaf9d95ab4e0ca183d6ced62a8fce2a9728aad22c596b39f7fc8e8b02309ac5700503df31558da8f2e548eb4a7b5ba62c717b53fe7fbfe4e763b00c4193366cc06e6eb48956f0541f07b0d1f374558ca4c8dff5af96a5757d7ae7c954972ec42ab62fb770c5db16b6395a69c0f2f7ef39bdfcc3ece5dffc95e8b2af5c1c3689ab92e16f77ce6c37d6678c2092714bc6614fbba5ba90f1656f2bc49a545ae8da55aadb699a8e0ea273ff949387bf6ec6cb192d9d799f52e2ab82ab5be9ac7f6fa6d5f777dcf7bde932d90b2a715dbb64a7d8876b0d72bf5b346f37deb5bdfca4ecbdf07e4bf7e35619b015a87f691db29af9a0f4cd8d35a19fd640000d030a54e903ff399cf644f2ecd49aeb91df269a79d16ce9831a3e0c4d5e4d1471fcd5e0cda7befbdb39f0a329fc8fbc52f7e919d1615501d7ffcf1d9e225f39cf90557e6933de6f6cdd75f7f7df88f7ffca3e473e5a7d4f3463fa3b9fdf3e5975f1eeebffffed94f0b9ae72e675afeef664ef8cd27fa0e3cf0c0eca793cc6da1dff9ce77864b962cc94ebffdf6dbb3cb984f4c4577cd2ae7676be1822b63f4b0c2a22b7387aa728bad22f672f6f3d6041d762481f50a00aaa3fde74465119fe88c57ecf8a2f629fccd000c457b7bfb48ed473eedfbfe51daa75c1904c1331aae501ed4f82fd5dee579decec32a3faf430cbbd0aad8fe1d4357eada5825a9e4c38ba6c8c33c36c559a53e7868ae879969d187feece7335f4bf6fef7bf3ffb4140731776fb754dec0291521f2cace479934a0b5d1b4bad5a6d3351c1d519679c917d6c8a91cc355a7347a9a8e0aad4fa6a1edbebb7bdad152bb82ab66d95fa10ed60af57ea67b57faefc7d40feeb5713b619a075a8cf7db4fa84e6c3e7c8433f190000344ca913647341c57c97fce69b6f9e3d99348546c50aae4caeb8e28a70a79d76cade5ad9dce9293a595eb06041f8c94f7e32fb1ce6f6d0e6ce52f927aae68292798d4d37dd345bd855eab9f253ea79a39f71ecd8b1d9622e73cbf69ffef4a7654fb37fb7abaeba2a7ba1c9dcdadabce6830f3e3830edcd37df0cb7de7aebec72a638abdc9fcd7e0d93163b411e3dacb6c551b57ebe0174d89104d62b00a88cf904a7f69d1728733ccffb803d1dfd8a1d5fd47e99b9238ddd0e0043d1d5d5b5a9f62f6ddabf1ca7e1f5caf3cad22008a62a672a1dda67bfcf5e0e83b30bad8aeddf3174a5ae8d559a723fbc985f6c51ea8387e6fa93994ffda0ecf399e77df1c517079eefa4934e0a67cd9a156eb8e186e121871c52f0f398d81f742cf5c1c252cfbb7cf9f270ca9429e1abafbe5a74dc7eed6ad262d7c652a956db8c5d7065ae458f1a352adb16155c955a5fcd747bfdb6b7b5620557c5b6ad521fa21decf54afdacf6cf45c115806a74f67fd0e1bfecf656473f190000344cad4e905d4b74127bca29a75434add169c113e4d1c36a532455abe78945871d4960bd0280f2799eb7b9f69b53943bcd3ff7ede9784bb1e38bda4f534eb5db01a0d6cc3edbf7fd83b4cf3949b9457949794db9370882339443b52f7fb7bd1cd666175a15dbbf63e86a796dacdc0f2fdac516a53e7868bea6cc148d98e733851f7d7d7d05cf77ce39e7641f9b0211fb678afba063b10f16967a5ef3b568e6392ebae8a2a2e3f66b579316bc36963ab5da66ec822b93e82e5151c19549b1f5d5c45ebfed75b858c195791cb76d95fa106d39af57ec67b5e7b3f7014309db0cd01ad417dc46797de2c489ebd9d35a1dfd640000d030b53a41762df6496cb9d31a9d163d411e3d6c68c552435d7e5074d89104d62b00288fe779bb689ff92fdff727f5f6f6ae634fc7da8a1d5f8220385cd32eb5db01a01eb40f7aa7f6418728bdca9dca22a54fb943393593c91cdcddddbd95bd5c2b8bf6e7f610b5d7acd7c6d29a16bd36962a6c336e856d06680dbeef1fa1fee0e5763be827030080066ad613e4524555a5a6353a2d7c825c6dd154b5cb55840e3b92c07a050083f37dff0bda5f2e0e82609c3d0df18a1d5fccdd66f4779c6cb70340a3687fb59d7298f213e53ee575e545e566edaf4e540ecc64329bd9cbb50abbd0aad8fe1d43d7acd7c6d29a16be36961a6c336e856d06680dea0b4e37d748ec76d04f0600000dc409b25b69f113e44a8ba72a9dbf6a74d89104d62b0028cdf7fd13b4af7c2993c97cd29e86e28a1d5f8220f8a0a63d6bb703804bb4cf7fbff655be7296324d59a6fc4bb94efbb1ef2ba3c78e1dbb89bd5c33b20bad8aeddf31745c1b732b2d7e6d2c15d866dc0adb0cd0fc72778b5d3266cc980dec69a09f0c00001a881364b7c20972d94554e5ce571374d89104d62b0088d7d3d3b3a1f691572b33956dece928add8f1a5bbbb7b234d7bd36e0700c70d3705a3e64e87da879da73ca4ac509e56ae508e54f6f13cef6df6826917edcfed216a8f6b636e856b63ee639b712b6c3340f3533ff09bea0f5f65b7a31ffd640000d0309c20bb154e90b3062ba61a6c7acdd161471258af00a090e779db6aff38d35c48348557f6740caed4f145d35ed3df760bbb1d00d244c78a75b52fdb4dfbb4af29172a8f28ab94d9caa5e61f52ca2734dffaf6b26912edcfed216a8f6b636e856b63ee639b712b6c3340f3533ff07ef57f0fb5dbd18f7e32000068184e90dd0a27c8038a1555156b4f141d762481f50a00d6a6fde25eca02e5787b1aca57eaf8e2fbfe2ce563763b00a49d29aeca64321fcf155b5da23cd6d95f84658ab17ea37cdd1469b5b5b58db0977555b43fb787a83dae8db915ae8db98f6dc6adb0cd00cdcd7c684ae7f16ff0a1b4e2e82703008086e104d9ad7082bc16bbb8ca7e5c3774d89104d62b00788bf689ddca62dff7bf604f43654a1d5f34ed763e150ba05598af19d43e6f6fe5bb9dfd5f3f384759a9fca5b3ffeb09bb75dcf9506f6fef3af6b22eb00bad4aeddf31345c1b732b5c1b731fdb8c5b619b019a9bfab287ab1f789ddd8eb7d04f0600000dc309b25be104b9405464756c6e58f7622b830e3b92c07a0500c386997f72074170a6f689cf799eb78b3d1d952b757cd1b40b9423ed7600681563c78edd44c79dd1caf7956bb54ffc97b24c99eefbfe2f34d4c0dfc15eae11ec42ab52fb770c0dd7c6dc0ad7c6dcc736e356d86680e6a6bee93dea077ec56ec75be82703008086e104d9ad70825c68e79d773e2ad7616e48b19541871d497075bd0ac370fdb973e7def0f0c30fffcf942953b2fb2552dfe8effe9f1933662c9c3a75ea17edf70768265d5d5d9bfabeff27ed0feff73c6f737b3aaa53eaf8a2bff771a6a0c06e07805696c96436ebe8e8f85c1004276a1f7ab3f282b244b94ff9a9f69b5fd2703b7bb9a4d98556a5f6ef181ad307b7afcf90c6c5bc1ff67b04b7b0cdb815b619a079997eaafa804bdbdbdb47dad3f016fac90000a0613841762b9c20175267795aa33bcc8d7e7d342757d7abb973e7de3863c68c70f1e2c5e1ead5ab0bf65324f998bfbbf9fb4f9f3e7d998e0b87d8ef11d00c3299cc8eda0f3eedfbfef96d6d6d23ece9a85ea9e38ba6f94110dc60b70300d6d6ddddbd958e5507eb38f523ed3bef50fa3afbbffad6140af79aafc0f53cef5df672b564175a95dabf6368b836e656b836e63eb619b7c23603342ff5ff7ad4efbcc96ec7dae82703008086e104d9ad7082bc367594db4c673997367b7abdd06147125c5dafcc9dad4cb18fbd7f22f54f5f5fdf6b3a2eccb4df2320ed82203850fbc045ca37ec6918ba52c717fdedf7d6f487ed7600c0e03ccfdb56fbd1439533725fedf29af292c66f557ea81c54cb3b3646fb737b88dae3da985be1da98fbd866dc0adb0cd0bcd4ffbb537dcc4ebb1d6ba39f0c00001a861364b7c209f2da3a7377b7ca659a3dbd5ee8b02309aeae57e66b04b9b3951b31ef838e0b6becf7084833dff7cd57052f54f6b3a7a1364a1d5fbababadeade92fdbed0080ea789ef73e1ddbbc2008ce54a66a1fbb5499ab5c6fbec6b5a3a3e3b3e62b74ede5ca11edcfed216a8f6b636e856b63ee639b712b6c334073523ff3edeaff2dd370637b1ad6463f190000340c27c86e8513e4b774ae7d77ab86dee58a0e3b92e0ea7ac571c1ad705c40b3f03c6f7dedf72e511ecb6432efb5a7a3764a1d5f7a7b7bd7d1f47f4f9c38713d7b1a00a02686eb98b7b3effb5dcab9dae73ea8ac0882e019e52a5378dcd1d1f1e9f6f6f691f682b6687f6e0f517b9c03b915ce81dcc736e356d86680e6a4be5fb7729bdd8e42f493010040c37082ec5638417e4be7da77b76ae85daee8b02309aeae57951c17fa96ac0c8fb9f6b16cccb83d9d0c3d1c17d00cbabbbbb7cafdc3f9668d6f644f476d0d767cd1f4f9e68e2c763b002019dae7aeebfbfe4794af2abfd67ef86fca2a8d3f1e04c1ef956f6b7ccf3163c66c90bf5cb43fb787a8bd4ace8148f2e11cc87d6c336e856d06684eeafbddae7ee238bb1d85e82703008086e104d9ad7082dcaf33feee560dbbcb151d7624c1d5f5aad47161cd9a7f87af2f7f73e0f18ca7170d145c99f1a8fd95a5abc2a52bde9a8f541f8e0b48bb4c26b387f677f3955e3d1c6e4f47ed0d767cd1f41941108cb6db0100f563ee34e8fbfec7b44f9ea8e1c5ca2c8daf52feaef1df6a3f7db8d99fb7b5b58d88f6eb83eddf51bd52e740a4fee11cc87d6c336e856d06683e63c78edda4b3ffeb04df6e4f4321fac90000a061a64c99f29fd5ab57179ca891fa47efc3429d20afb1dfa356d4197f77ab86dde58a0e3b92e0ea7a55ecc2a929b6baeaa179e189373e1edef7c4c270fa9cbef0945b9e1c28b832e3a6fd4fb3168427689e73ee79365cb692a2aba1860ba74833ede7bea2bc6286f6342467b0e38bf94a2bcdd36db703001aaba7a76743dff73fa51ca1fdf4e5b9f3df95b9e10566e879de2ee6eb61ed653134c5ce814863c23990fbd866dc0adb0cd07c74de1ea8ef77a7dd8e78835d0701000048cc8c1933162e5ebcb8e0448dd43f2fbcf0c2753a419e69bf47ada6b3f4ddada2b4d9cb25890e3b92e0ea7a55ecc2e96bcb5685c7dff0f84081d56039f6bad9e14baf2d2f781e5259b8708a941a1e04c18fb59f9b6fee70654f44b2063bbe68fa4f9593ec7600805bccfedcf3bc8dcdd0f7fd6372e7c2ffd43176b9f28072b6f9679ce6f9c030ee2239247c18d19df061c474b8efbefbd8661c09db0cd09cd4e7bb59e9b1db116fb0eb2000000089993a75ea17a74f9fbeacafafefb56a4f94972fe71fea4389feee7df3e7cfbf4627c72f2a87d8ef51abe92c7d77ab2875bdcb151d7624c1d5f5aa58c195c95d8fbd5c5058552c37fced8582e549e5a1e00a69d3ddddbd51eec2e09f35be953d1dc91becf8e2fbfeb7ccd755d9ed0000b744fb737bd8d3d3334ae30728c76b7f7e9386f3357c43c3fb8320f899f2e54c26f3defce742697c18d19df06144b7b5b7b78f341facf8fad7bffeff162c5850f0fe91fa876d06683eb9eb2a4bd59fdbcc9e8678835d07010000489429f2312766cabfcd3f7607cb5d77dd155e72c925e18f7ffce3f0e8a38f0e0f3ffcf0f0eebbef2e98af9e311d2abb2d45317f77f3f76ff962ab621add616ef4eba339b9ba5e99fd927d012f8af91ac1a8a0eab8eb6787d7fc657ef8fca2a5d99871d3164dbf7bf6cb05cb93ca63de0ffb3d025c65feb9ab7ddb6cdff72ff63c6f7d7b3aea63b0e38ba61fa2dc65b70300dc62175a95dabfebb8bba58ebf6334cf29caedcac2cefeaff5bd4b392d0882f671e3c66d6d2f877eb5f83022195af830a2f3866b1fd3a5fdc98bcad5975f7ef9d7d9661a1bb619a079697feb29f7d8ed28ae543f190000c009994ce693eae4fdd0dcb65e9d97ff311d985c969969f6fcf54687aab935fafd6df4eba339b9ba5ec5155cf52d5919ce787a5178ca2d4f0e145499022b7b3ed3164d37f39a65ccb2f67ca4fc507085b4e8e8e8f84cee9fbb47dad3505f831d5fbababa76d53c4fdaed0000b744fb737b582ecdbf8d3256395db95b79557959b94d39390882ff52b6b0976b55a66021f761b8b23e8c486a1e3e8ce828ed2ff6521e56fea67dc6de51bb79af72ef19db4c63c236033429ed6fafd7fef670bb1dc555da4f060000489ce779dbaa93d2a35ca3bc663a2c31f98fe6fbb8bd6c23d0a16a6e8d7e7f1bfdfa684eaeae57e6c29d5df4637f5da089b9ab953d9f69b3e733b1e723e5c7bc1ff67b04b846fbb389caa28e8e8ecfd9d3507f831d5fd47f7f7b1004cbed7600805ba2fdb93d1c0a3dc7f6ca57949f2b53725f45384fb951c7861f28fb9be384bd1c80d693bb367ca5b240fb8af16a1a6ecf0300a82ded7bdfa6fdee528ae22b538b7e320000404da94377a83a29ffcf74544ae43a7bb946a143d5dc1afdfe36faf5d19c5c5daf28b8722b145cc1656d6d6d23b42fbb4099e379de07ece9688c728e2f9a67594f4fcf28bb1d00e08e687f6e0f6b6c782693d951cf9d51ce516698a25c0d9f55ae56bea7c7fb7677776f642f08a039e5fed96fbe9ed47c00f774b67f00a81fed770f53eeb7db515a42fd64000080a10982e0bf4d47a5583a3a3a3e6d2fd32874a89a5ba3dfdf46bf3e9a93abeb555cc1155f29d8b850700557799eb7b9f66353943bbbbaba36b5a7a371ca39be689e27cc570bdaed00007744fb737b98b4dedede758220f8b03241aff92be5afca4ae549e532e53bca5e3d3d3d1bdacb024837dff7b57977bea05c9fc964de6b4f070024abb3bfe0fd9b763b4aab573f190000a062eaa8f49ace8a9d20089eb2e76d243a54cdadd1ef6fa35f1fcdc9d5f52aaee02acaf4398b060aaa8ebb7e76b6c0cadcd5cac48c9bb668ba99d75e9e541e0aaee022cff376d13eec5fbeef4f32ff94b5a7a3b1ca39be689ebb9443ec7600803ba2fdb93d6c84891327aea7d7ffa8f20de577ca3f9455b9e145b9f68f9af9ec6501b84fdbef2794079547cd5dedece90080e48d19336603f375cfdddddd5bd9d3505a23fbc900000083d289f655a6c3921f75fc8eb1e76b243a54cdadd1ef6fa35f1fcdc9d5f5aa54c1d53d8fbf5cf07581c572e7ac0505cb93ca43c1155ca33ee017b4ff5aacfee1387b1adc50cef145efe36f956fd9ed00007744fb737be80af34fc14c26f3c9cefe3b5e993b5f3dd1d97f272c73472c7367ac1e73a72c8ab301778d1b376eebdcf6fbb2fa865f657b0580c651bfa95dfbe3e9763b06e75a3f1900006080e7793babb332d7dcd1ca745a7259a3c75bd8f336121daae6d6e8f7b7d1af8fe6e4ea7a55ace0eab565abc2136f7cbca0b0aa587e70c3ecf095a5ab0a9e8754160aaee012dff74fd0beeb25f3cf557b1adc51cef145f39ca4fefccfec7600803ba2fdb93d74597777f746e6ee38cad19dfd5f89f3acb242f9b3728e92517f6227cd3adc5e1640fd98af0435fd41e555d3271c3b76ec26f63c0080fad2fef80fda2f7fc76ec7e0d2d04f0600002d489d94bd943e75f426789ef7b6cefe5b4b9b82abebec791b8d0e55736bf4fbdbe8d747737275bd2a5670b5eacd35e16fa63e97fddac05b1f7d319c36675178ca2d4f0e145899f1e973fac2fb9e58982dccfafd8ce7c3d56bd6143c0fa92c145cc105b97fc8987f9ace54b6b1a7c32de51c5fcc1dcacc7b6ab70300dc11edcfed61da789ef7761d77f6577ea0dca0df639eb2549962be9e58c3af28dbdbcb0148466e9b9ba7edef266d9fefb3a70300ea2ff7f5cdaf9b3b0fdad330b8b4f69301004013cb643207ab93f28a4ebec7e4b56d66ee74d5d1d1f1b9fc795d4087aab935fafd6df4eba339b9ba5e152bb8323145578bde5839f078c6d38b060aaecc78d4be64f99b145bd528145ca1d13ccfdb56fbab99ea035e650aafece9704f39c717f5e73fa3f9fe6cb70300dc11edcfed6133183f7efc3b3299cce7f53b9dacdca62c505e55ee564ef77dff8b9d14790335a56deaa3ead33fa0ed6b9686a3ede90080c631ff87e31cbd7acdd44f0600004d409d931ea52feeeb627215f6ceddfa9d0e55736bf4fbdbe8d747737275bd2a557065a76fc9ca81822b336e4f27430f055768a4cefebb9d9a7f801e6f4f83bbca39be689eed95f9763b00c01dd1fedc1e362b73bd29088276e5c7fa5def525e5116fabeff470d4f31ff88f43c6f4b7b3900a5699b7aa7b6a14b943ee51bbdbdbdebd8f300001a4bfbe74bd5d739ca6e47799abd9f0c0000524427e127aa7332579dbb9dec692ea343d5dc1afdfe36faf5d19c5c5daf2a29b822c987822b348af651ddca62f509bf604f83dbca39bee4beaee0df9ee7ad6b4f0300b821da9fdbc356a2e3d47b8220f8b2f23365b2fe064b94f9e6abd09413347e404f4fcf287b3900c3868d19336603f3559eda4e5ed5f0ccaeaeae4ded7900008dd7d6d636c2ecab95edec69284f2bf69301008063cca79bd429b940792c8ddf134d87aab935fafd6df4eba339b9ba5e5170e55628b842bd993ea1f9878cf651cf799eb78b3d1dee2bf7f8a2f95eeeeaea7ab7dd0e007043b43fb787ad4efd930ff8bedfa9fecad9e6ebd194e5a6dfa25ca3f663ccd7e66a9e8dede58056a2ede130e55fca6dda2e76b0a70300dca1becc81da5f3f6cb7a37cf49301004043e53ef174833235ad9f76a243d5dc1afdfe36faf5d19c5c5daf28b8722b145ca19e4c3fd0f7fd3f69ff74bfe7799bdbd3910ee51e5fcc055d651fbb1d00e086687f6e0fb136532c6e8ac4d58719af9c9f3bbead0c82e029e50f6a3b42f9544f4fcf86f6b240b3d13abf9bb9beab75fe716d0707d8d30100eed1fefa77da777fdf6e47f9e8270300808631ff585367649a72bde779ebdbd3d3820e55736bf4fbdbe8d747737275bda2e0caad5070857ac964323b6abff4b4f947a5b99dbd3d1de951eef1c5f4ffcd1d42ec7600801bec42ab72f7efe8ff6a1e1de3760f82e0700d7fabbfdda3ca2a8dcf522e56fb7f6bf831f315bbf6b2401a799eb7a5d6f1df298b946ff2b5d100900e667fadfdf662657b7b1aca473f1900003484f9ea4075441e33ff58339f08b4a7a7091daae6d6e8f7b7d1af8fe654eff5aab3bfb8b6cd6eb75170e55628b8423de46e5f6ffe39f30d7b1ad2a7dce38be63b4beffd0fec7600801ba2fdb93d4475ccdddd7ddfdf53f9968e7fbfcfdd0168953253e3bf56bedad5d5b52b852a4813f3e159735714adc7af2ae7f4f4f48cb2e70100b8aba3a3e3b3da7f3f62b7a332f493010040dde9847c677542e6fabe7f823d2d8de85035b746bfbf8d7e7d34a77aaf57e6f57229597845c1955ba1e00a49535ff028ed13162afbd9d3904ee51e5f8220f8aee6bdc06e0700b821da9fdb43d44e7b7bfbc88e8e8e4fe7fa4357ead8f88c862b940735fe4bb57799eb679a75b8bd2cd0685a47dbb5aefe53b943ebea4ef6740080fb4cd1b7f6e3c7dbeda80cfd6400005057ea7ceca5f4e9c47c823d2dade85035b746bfbf8d7e7d34a77aaf57e6f5acc4165e5170e55628b84252cca7e1b50fb844792c93c9bcd79e8ef42af7f8a273814335efed763b00c00dd1fedc1e22595d5d5d9b9af324dff78fd3f07ae57965a98e9b539533950ef5a3de672f07d48bd6cd8f683d9cac3ca5f183ece900807430df3aa33e469ff6e53bd8d35019fac90000a06e3299ccc1ea7cbca24edc187b5a9ad1a16a6e8d7e7f1bfdfa684ef55eafcceb15c95a8557145cb9150aae9084eeeeeeadb4dd3fa8dcacf18dece948b7728f2f3a1ff89832cb6e0700b821da9fdb43d49fe7799b9bc216bd072729b7282f29af29f7064170862962eeeaea7ab7bd1c504be3c78f7f47ee4e288b95efb4b5b58db0e70100a487f6e5fb714e5e1bf4930100405da8d3d1a3f46532994fdad3d28e0e55736bf4fbdbe8d74773aaf77a655e6f90640baf28b8722b145ca1d6d40fdc43dbfa7ca577185f8fd394ca3dbe0441b085e67ddd6e0700b821da9fdb43b841c7d177ea3d39c4f4a9943b95454a9f728772aaf9c0a32972b797032a3571e2c4f5b4be1dadf5ea15e53cad5b9bd9f30000d2c7ecd37ddfffa1dd8ecad14f06000089d389f989ea74cc55076e277b5a33a043d5dc1afdfe36faf5d19ccc7ae56228b8722bf6fb43488df2157b9f84e661de63bbad18cdbb8abb9c01809ba2fdb93d84bbf41e6da71ca6fc44b94f795d7951b9d95c97530ea458069530857b5a6f9ed13a7497861fb4a70300526bb8f6ed0b3ccfdbd99e80cad14f06000089c97d0ff405ca63e3c68ddbda9ede2ce85035b746bfbf8d7e7da016cc7a5c24d33af94a4167c31dae506bededed23ed3634974afa2de61f78beef7fc86e0700345eb43fb78748974c26f37ebd77be7256eedc6b99f22fe53a1d87bfaf8c1e3b76ec26f672686da67fa675e46ee5698d8fb1a70300d24dc7ffbdb58f7fc26e4775e827030080448c1933660375dc6e50a67675756d6a4f6f2674a89a5ba3dfdf46bf3e500b663db6b256a15584822bb742c115804a55d26fd1bcf7f9be7f90dd0e0068bc687f6e0f917ac3cd9d8a94717a4fcf531e5256284f2b5728472afb789ef7367b41343fbdef9bab6f76bed68157343caaadad6d843d0f0020fdd40f385bfbfa53ed7654877e320000a8395360d5d9ffcff4eb75b2bebe3dbdd9d0a16a6e8d7e7f1bfdfa402d98f53897d842ab48230aaece3fff7cb38d859b6cb249b874e9d282e971993e7d7a78f5d5570f3c5eb06041386ad4a870d2a44905f3961bfb395d080557002a5549bf45f35e1a04c1e1763b00a0f1a2fdb93d44f3f13c6f5d1d8f77d37bfc35e542e511659532db1cab956f2a9f6885eb7badca1456f9be7f84dee7c5caafc68f1fff0e7b1e0040f3d0befe051dfb3f6cb7a33af4930100404d99af0e5407e331f38928f39582f6f4664487aab935fafd6df4eb03b5d03948a155a4110557fbedb75fb8fdf6db678baeaebdf6da82e97139f8e083c3c30e3b6cadb669d3a6857d7d7d05f3969bb8e76c7428b80250a94afa2d9af754e534bb1d00d078d1fedc1ea23598e2aa4c26f3f1cefe62ab4bcc75becefe222c538cf51be5eba6488b3b20a59fdee7cfebbd7c4aefe97dfcf31d009a9feffb7b6a9fffb4dd8eead14f06000035e379deceea5ccc55a7ed047b5a33a343d5dc1afdfe36faf5817aaa77c1d5bc79f3c2e1c38787a79f7e7ab8dd76db85871e7ae85ad32fbdf4d270871d760837d8608370c71d770c9f79e699f0a4934eca166745b9ecb2cbc23973e664c74f3ef9e470f7dd77cf3ed79a356bb2cfd1dddd1d8e1831227ce9a597c2a38e3a2adc669b6dc20d37dc30dc73cf3dc3bffffdefd979e29ed3b45f74d145d962b091234786fbeebb6f386bd6ac82df21c9507005a05295f45b74cef055cd7fb9dd0e00f8ffecdd0b9c1c559df0fd2490107181102e2ef0803c0612975d2ebb0a028b6604344f700741b7a7521d928c3c92ddc79715cc13e42e030b2897e51278502e2e18254bc24bc0655570cccc6404b9845bb804141d02840c81700d21713fcfeb79cfa94ccd76fe5d5dd35ddd5575aaeaf7fd7cfe9f499f5397ae3ea74f9daefea73a7dfe782effa2b8cccf0cbaae7b848e7f9ab1e5e70757e9f840c76f666cf979c259fadcfe1745f9cf9759a7db6ab26eb3ffd0f13bdda6edb21e00904f7afcbf4c8ffdff2ccb111df3640000d0127a52f1191d83fa43fa1c5997774ca8f22dedf64d7bff4092924eb8bafcf2cbbd04a7471f7d549d72ca295e62d59b6fbee9d5ad58b1428d1933464d9d3ad5fba9bfaeae2eb569d326b57cf9723576ec5875e49147aabbefbedb4bdaaa4cb8baeaaaabbc7f9be5366cd8e0fdd4607b7bbbb7cd7beeb947dd78e38deada6baff57ec270faf4e95e79d036cdfe4d32984902bbe1861bd481071ea80e39e490aa63883348b802d0a846e62d7ad963f467871e590e00489f3f9ecbbf40a5e38f3f7e077d2e9faae37febf837dd4f7eafe33d1d7d8ee35ca9ffea3fce24b91ed2d3d9d93941b7cb553aded06d336feedcb963e5320080fcd2e3ff1ff4f87fb02c4774cc93010040d3cae5f271431fd4a7cbba226042956f69b76fdafb07929474c2d5a73ef529b5ebaebbaa575e7945dd72cb2d5ea294f96beafc64ac9e9e9eaaf54c6256e5cfff55265cad5dbb568d1b374e9d7aeaa96ae9d2a55ef95d77dda5d6ad5ba78e3df658ef6e57a6cc84b91b56ad6dfafbaf0c9300b671e3c6aae71357907005a0518dcc5bf46788fdcd17b3b21c00903e9968d5c8f88e62d3e7f79d3b3a3a8e755df76cdd6feed2f1b28eb775fc52c7a58ee37c45ffdd5bae8778954aa56df46bffbff46bffbafefb03fd7837b90c0020dff4f8ff37fa3cf0a22c47739827030080a6e8c944a78ec172b97c98ac2b0a2654f99676fba6bd7f204949265c3dfffcf355094d26bef0852f78f5575e79a5f7b8b7b7b76a5d991c559970651e9bbabdf6da4bcd993347edbefbee5e92949f4075fae9a77b894ce6a7020f3ae8a09adbbcecb2cbbce5172c58e0257df9e1ff54611241c215804635326fe9ecec1caf97dfa4ff395ad60100d2e58fe7f22f10c5ac59b37637ff59d3719cefe8be74afb98ea8639d7efc33fdb74bfffdbb52a9f4e7723db4867e8d8fd1f18c8e65aeeb1e24eb0100c5a0cf0397eaf3c07765399ac33c1900004436f4bfd5061cc7992ceb8a840955bea5ddbe69ef1f4852920957e62702f52ed5f7bef73d75efbdf77af1d9cf7e566db3cd366acd9a35ea81071ef0eadbdadad4a2458bd459679de5dd09cbac6bee8a3569d224b578f162f5c4134f54255cfdf4a73ff51eefb6db6e6adebc795e999f4075e69967aa2baeb8c2bbd35565c295dce6638f3de6ddd1ea88238ef07ed2d0fcaca0490293c71167907005a0518dce5bcc97adfa33c5c7643900205dfe782eff02ad522a95f6d2738013745cec38ce7dba8fadd7f1aafef7dd3aced5f145bdcc44b91eeaa75fbffdf46b7a8f8edf9bd75ad603008a459f0f7e572e973f25cbd11ce6c90000a0615d5d5d63f424e23a1d4f9d74d2497bc8fa3cd3c7dc6b26502344af5c0fd995f68439edfd03494a32e1ea80030e5013264c501f7cf0c17099ffb382d75e7badf7f8eaabaf56fbecb38f77f729931c353838e8959bbb4e4d9c3851edb8e38e5e32964cb8faf0c30fd51e7bece195ad5cb9d22b33495c871d7698b7ad69d3a6a9d34e3b6dab842bb94d53b670e1423579f2643576ec58ef4e59175f7c71d571c419245c016854a3f316bdfc638ee31c2acb0100e9f2c773f9178853a954faef7a5e50725df7721d3dbadfbdab6340c7625d7e464747c7e767ce9cb9a35c0f5b33af917ecdaed0f1a67e1dbfad5fd77172190040b1983b1c9a73aa2c47f39827030080864c9f3e7d3b3d395b622e7c14f122879e3cb59909d408d126d74376a53d614e7bff4092924cb822460e12ae0034aad1798b5e7ea9fe5cf155590e0048973f9ecbbf40c246974aa5298ee3ccd471b5ee870fe8d8a0e70e2fe8f8892e3bada3a3e36fdbdbdbb7972b16d1d07f903d45c7a07e6d6ee62ea200009f3e375ca4e30a598ee6314f060000751bfa1f52bd3a1617f97f470dbd0632c98abb5be554da13e6b4f70f2489842bbb82842b008d6a74dee2baee357a9d6fc9720040bafcf15cfe05d2562a95b6711ce7af747c4dc7ffd17df3111d1bf5bf9fd6f38a7fd5f10d73f74cf31f46e5ba793663cb7f107d4a475fb95c3e44d603008a4d9f1f56e9f88c2c47f39827030080ba989f0e341fdc1dc75960fec794ac2f92a18b1832d18abb5be554da13e6b4f70f2489842bbb82842b008d6a74dea23f5bcc3377ac90e5008074f9e3b9fc0bd868eedcb963f57ce26f743f9d6beeeca4e349fdef8d3a1ed7fffe81ebba5f3749486d6d6ddbca75b34e1fd727f471dea56380bb86020082944aa503f479e215fdcfd1b20ecd639e0c000046646edf6d3eb83b8e7396ac2baa19c177b9ea95cb21fbd29e30a7bd7f2049245cd915245c016854a3f316bdfcdfebcf18ffaf2c0700a4cb1fcfe55f202b3a3b3bc7eb39c6e13a4ed5fdf736d7759fd37f3fd0f1908eeb74f96cf3057456ff53e9f1c71fbf833e8eefe978531fdbd945bba31700a07efa9cf71dfea3537c9827030080507ab2f0191d83fac3fb1c5957643382ef72d5269743f6a53d614e7bff409248b8b22b48b802d0a846e72de572f930bdcea3b21c00902e7f3c977f812c2b954a7fa6fbf267cd1d36f5df453a7ee7baeefb3a96ebf8171dae5e66bf5116df01c42488e9e77db28ed774dc6a7e91402e030040257dde7bbaa3a3e36f65395a8379320000a8a95c2e1fa7270b6fe809d97459076f22557997ab5e598f7c487bc29cf6fe812491706557907005a0518dce5b867eb6fc75590e0048973f9ecbbf40de7476764ed0fdfb181d679abb6eeabfabf5df77f4df5fb9aefb5df3337de572f9e372bd34e8e7f4591d8febf8b57e4e9f92f5000048fa9c36599f375e1b65713271d6314f06000081f424a153c7a0f95fe7b20e5bccd8fa2e576db21ef990f68439edfd034922e1caae20e10a40a322cc5b46eb7536f333380060177f3c977f812228954abb99ff7caafbfdf93a7eaa63ad8e3774fc5cc745aeebb62779672993f0a5f7b944ef7bb50e47d60300508b3e6f9ca3e33a598ed6619e0c0000aae80ff167eb49c280c97e9775d8da8ca1bb5cc972e447daed9bf6fe812491706557907005a05151e62d7a9d3f0cfd7c0f00c012fe782eff0245a5df037bea385ec73febf8858e37676cf959bf7b749ce7baeeffd0b1ab5caf19b366cdfaa8dee6c57afbebcd3ef47ce92372190000c2e8f3c713fa5c325596a3759827030080615d5d5d63f4e4e03a1d4f25f93fb5b24cbf566da5528909558ea53d614e7bffb528a5c60d0c0c2c79e8a187fe73d9b2655e6206916ce8d7fd4ffdfdfd6b7b7a7abe2cdb27abcc71c9a41f22bd30ed21db0800c24499b7e8757a5dd73d5a960300d2e38fe7f22f80ffa2df17fbeaf87b1ddfd3b16ce8a7085fd271a79edb7cdbcc6f4aa5d24e72bd3a8cd6ebced1db59a363a1dec65e7201000046522e973fa1cf23af9beffd641d5a8779320000f0989ff130b7a7d6d13373e6cc1d653d6a9a3a7efc7833a1e27f09e454da13e6b4f75fcbc0c0c09dfdfdfd6addba756ad3a64d55891a44fc615e77f3faf7f5f5bdd7ddddfd25d9465944c2955d41c21580464599b7e8cf1f3f325f2aca7200407a64a25594f11d28a0d1e572797ffd7e29ebb84a47bf9ee3bcaffffe56c7ed3abea51f1f65ee5c2557f4e9658ed4f1a88e87f4b60e93f50000d4cb24ffeaf3c90db21cadc53c1900008c32095633b6fc34dee252a9344ed6a3269364b54ec7fca1bf245de550da13e6b4f75f8bb9b39549f691091a44f2313838b8bebbbbfb51d9465944c2955d41c21580464599b7ccd8f2d33ce7cb7200407afcf15cfe05d018735711d775ff72e88e55d7eb7858c7073a9ed571ab8eff47c7674e3ae9a4fdf4df453a5ed151d6ab8e96db0200a011fa7cf22877938e1ff36400000acefc74a09e103ce538ce026e2dda103fd9ca4fb2928f9113694f98d3de7f2de66704b9b3951d61daa1bbbb7bb36ca32c22e1caae20e10a40a3a2cc5bf43a73f567919b653900203dfe782eff0268dedcb973c7eaf7d45feb3845c78de6baac8eff4fc745edededdbcbe501006854b95cfeb83eafbc512a95b69175682de6c9000014989e6c4dd1938101c771ce927508552bb9aa5639322ced0973dafbaf85c418bb222f8931f42bbb222ffd0a4072a2cc5b5cd7fd1f7abdfb653900203d32d12acaf80ea07ee6d7076419000051398e334fcfdf6e92e5683de6c9000014949e047c46c7a0b9a5b5ac43a89192aa46aa47c6a43d614e7bffb58c9418b3e20f6faa33973cade6fddb53a171c5cf5f506bdfda50b53ed158e4253166a47e45241b79e95700921365de522a950ed0eb3d2fcb0100e991895651c677000000a443cfdd7ee338ce1765395a8f7932000005542e978fd3938037f4846bbaac43a87a93a9ea5d0e1990f68439edfdd712961873d69d4fab477eff86ea7e766d55825550dcd8fbfbaa6d108d455e1263c2fa15917ce4a55f01484e94794ba954fa33bdde07b21c00901e996815657c07000040f266ce9cf9dff4dc6d7d5b5bdbb6b20eadc73c19008082d127ff4e1d83e572f9305987508d265135ba3c2c95f68439edfdd7129618e3dfb96acdfa0d55c95541f19da5cf546d83682cf2921813d6af88e4232ffd0a4072a2ce5bf47a6f974aa589b21c00900e996815757c07000040b2f4bced9baeebfeab2c473c9827030050207a9275b63ef90f388e3359d62154d4e4a9a8ebc122694f98d3de7f2d6189312689ea8cc52babca89f8222f8931cb962dfbd3a64d9baa8e8f483e743bacd5fd6ab36c2300081375dea2d77baa5c2e1f22cb0100e990895651c777000000244bcfdbfacdafdcc872c4837932000005d0d5d535469ff4af335f649c74d2497bc87a846a3669aad9f591b2b427cc69efbf969112aeea0db92e112df29270d5dfdfbf76ddba7555c747241f2fbffcf21dba5f3d2adb0800c2449db7e8f5eed571bc2c0700a443265a451ddf010000909c52a9f4e733b6dc417a9cac433c982703009073d3a74fdfce75dd253a7a66ce9cb9a3ac47a856254bb56a3b4841da13e6b4f75f0b095776455e12ae7a7a7abedcd7d7f7dee0e0e07aee74954ee8d77d70f5ead58b749f7a45c797641b014098a8f316c771fe8f8e53653900201d32d12aeaf80e000080e4b8aefb0d3d6f5b28cb111fe6c90000e49849b0d227fb5e1d8bc9686f58ab93a45abd3d2424ed0973dafbaf85842bbb222f09578649f2317756d2f147735c44e2615e77f3fa936c05a06151e72d7abd335dd7bd5c960300d22113ada28eef000000488eb9f1c20cee1e9d28e6c90000e494f9e9407da27fca719c05e62705653d42c9e4a8d13ae60cfd6d845c4f6e171990f68439edfdd762123364d28f1f32a92a2ce4ba44b430ed21db080080a4459db7b89a5ef70e590e0048874cb48a3abe0300002019a5526937c771dee9ecec1c2feb101fe6c90000e4909e584dd127f9013db93a4bd661443229ca244bfd5087993499bff5265dd55a4f6e1f964b7bc29cf6fe6b21e1caae20e10a006083a8f3968e8e8ebfd5eb3e28cb0100e990895651c7770000002443cfd7e6ea5824cb112fe6c90000e48c3eb97f46c7a0ebba73641d4614940c655e473361f2a39ea4abca642b3f6657d407ed07964a7bc29cf6fe6b21e1caae20e10a006083a8f316bddede3a5e95e5008074c844aba8e33b00000092a1e76bbf741ce72bb21cf1629e0c00408e94cbe5e3f4c9fd0d3da99a2eeb30a25a495041c95361495741cbdf32545ea9d6fe6099b427cc69efbf1612aeec0a12ae000036883a6f29954adbe875ffd8d6d6b6adac0300244f265a451ddf01000010bfd9b367efa2e76befeacfd61f91758817f364000072429fd43b750c96cbe5c3641d463452f25350125550d255d07241c956be91f6eb99356bd64765199293f68439edfdd7129470f59da5cf5425548585595e6e838816245c01006cd0ccbc45affb8afe2cf371590e00489e4cb46a667c07000040bcf45ced64d77597c872c48f7932000039a0275267eb93fa80e33893651d465457d2d3a8e064aacaa4aba0fab0642b5fe8fe4db2956edb97741b07d6237e694f98d3de7f2d41095737f6febe2aa92a2cccf2721b44b420e10a00608366e62d7add07747c56960300922713ad9a19df010000102f3d57fbb9ebba1db21cf1639e0c0040867575758dd127f3eb743c75d24927ed21eb31a2d064a700414955e6f19880f27a92ad7ca1cfc3245be9367e83a4ab74a43d614e7bffb504255cad7d6b83bae2e72f542556058559ce2c2fb741440b12ae0000366866dea2d75de438ce4c590e00489e4cb46a667c070000407c3a3b3b27e8b9dabbfc524a3a982703009051d3a74fdfcedc225447cfcc99337794f5185168925388a0a4abdf8ac78d245bf9429f0f4957e9497bc29cf6fe6b094ab822d20b12ae0000366866dea2d7fd9ee33867c9720040f264a25533e33b000000e2a33f47cfd673b5a5b21cc9609e0c00400699042b7d12efd5b1b8542a8d93f518516872531d8292aefc88926ce50b7d5e245da523ed0973dafbaf85842bbb82842b00800d9a99b7e839ee37f4fa37c8720040f264a25533e33b000000e2a3e769f7ea28cb7224837932000019637e3a509fc09f721c6781f94941596f1333d1b035f4d39b2f9f6f83cc6b2fef6c651e37db26f3e573b525e4132d8ab48f3dedfdd742c2955d41c21500c006cdcc5b5cd76dd7ebff872c0700244f5e0f20088220088220ec8de38f3f7e07399f4332cceb2fcb000080a54aa5d2147df21ec8ca4f6d583cd1983a2ae44e527508bbc395298fe50e57be34ee7465715bc62eed634f7bffb59070655790700500b04133f316fd19e7601d4fcb7200407afc71bd99f11d000000f1993e7dfa76b20cc9619e0c004046e893f667750cbaae3b47d6d9caf289465dc94d018292ade49daea2245d35f47c924ebab2bc2d6395f6b1a7bdff5a48b8b22b48b80200d8a099794bb95cde59afffae2c0700a487842b000000a036e6c9000064c0ecd9b377d127edffeb384e49d6d92c03138d86929c4605275bdd326acbcf08caf24692ae1a7d1e9ea1a4ab81f6f6f6ed655dab65a02d6393f6b1a7bdff5a48b8b22b48b80200d8a0d9798b5e7fc3cc99337794e5008074907005000000d4c63c1900808cd027edc5aeebfe932cb75946261af5263bd54ab6f293aa82eaeb49baaa77ff819248b63232d296b148fbd893debfde5faf8e36592e9170655790700500b041b3f316fd79e7391d7f29cb0100e920e10a000000a88d7932000019d1d1d1f1797de25e29cb6d96a189c648494f41c95495c956bea0e5c292ae46daaf3532d4962d97f6b127bd7fb3bfa1084dbc22e1caae20e10a00608366e72d7afd5f388e335d960300d241c215000000501bf3640000b263b43e71ffde719c436585ad3236d1a895fc14944415946ce50b5a3e28e9aad6feac94b1b66ca9b48f3de9fd57245c85265e9170655790700500b041b3f316bdfe4daeebfe832c0700a483842b000000a036e6c900006488ebba67eb93f78db2dc56199c68042541cd19557fb2952f28e96a76457dd07eac96c1b66c99b48f3de9fd07245c05265e9170655790700500b041b3f316bdfe793a2e91e5008074f8e37ab3e33b0000009047cc930100c890934e3a690f7df27e7bd6ac591f957536cae84443264355264fd5936ce5abb59edc7e2664b42d5b22ed634f7aff22c92a28bcc42b12aeec0a12ae0000366876dee238ce6cbd8d1fcb7200403afc71bdd9f11d000000c823e6c90000648c3e79ffd4719cafc9721b6578a22193a24cb294b94355bdc9563eb99edc6e6664b82d9b96f6b107243c5911245cd915b27dea09d9d700006856b3e717bd7e9bebbacb653900201dfeb8deecf80e000000e411f364000032c675dd767d027f5096db28e3138d562747b57a7b89ca785b36a568c72e93722aa2d77c09ea2f47c2955dd1e81dae8ad6af0100c968f6fc522e973fa1b73120cb0100e9f0c7f566c777000000208f982703009031a552691b7d027fcd719cbf9075b6c9c144a3554952adda4e6a72d0969115edd8474ab4f29170655790700500b041b3e717fd59679cdec6e6aeaeae31b20e00903c12ae00000080da982703009041fa047e89ebbaff22cb6d93938946b3c952cdae6f859cb46524453bf69112ad7c36245c6ddcb8d1b48d17db6db79dfae4273fa96ebef9e6aae564ac59b3464d9830415d76d9655575f5465f5f9fbafdf6dbabcad30a12ae00003668c5f9456f6350c79eb21c00903c12ae00000080da982703009041433fb5b1cefc0f705967931c4d34a2264d455dcf3a396acb8615edd8474ab4f2d9947075d4514779c94f471c71841a3d7ab47afcf1c7ab9695d1dbdbab060707abcaeb8de38e3b4e9d78e28955e56905095700001bb4e2fca2b7f188e33887cb720040f248b8020000006a639e0c004046e993f832c7714ab2dc26399b68349a3cd5e8f256cb595b36a4c8c71ec6a684ab52a9e43dbeebaebbbcc7b7dd769bf7f8a69b6e52fbeebbafda7efbedbda4ac279f7cd22b5fb56a95b7dc79e79d17ba9c891ffef0876ad2a449de1db4f6df7f7ff5c20b2fa873ce39c75bdf8f5b6fbdb5eab9251d245c01006cd08af38bdec69daeeb76c8720040f248b8020000006a639e0c004046b99a3e91df2fcb6d92c38986499eaa2789aadee53223876d59b7221f7b189b12ae4e38e104b57af56a3573e64cef0e572b56acf0c2fcdbd4dd70c30deac0030f54871c7288b75e65c255d872a66ecc98316aead4a9de1db4bababad4a64d9bd4f2e5cbd5d8b163d591471ea9eebefb6ef5d24b2f553db7a483842b00800d5a717e313f9daeb7335f9603009247c215000000501bf3640000326afaf4e9dbe913f99be572f9e3b2ce16399d688c944c35527d26e5b42deb52e4630f6353c255655c73cd355edde5975f5e556792a7cc3a95095761cbf9753d3d3d55fb3677bce227050100d85a2bce2f8ee39ca6b773ad2c0700248f842b000000a036e6c900006498ebbad7e8b85096db22c7138da9a38293aa6a95675e8edb7244453ef63036255c7dfef39f573ff8c10fbc7f5f75d5555edd65975de63d5eb060819730e5c7e6cd9bb74ab80a5beeca2baff4ea7a7b7babf64dc2150000d55a717ed1db38d1719cbb6539002079245c01000000b5314f060020c366ce9c79a03e99bfd2d5d53546d6d920e7130d935455995c251fe74acedb3254918f3d8c4d0957a552c97b7cf2c9277b89508f3cf2887aecb1c7bc3b551d71c411decf019a9f0b34095466b9ca84abb0e51e78e0016fb9b6b636b568d12275d65967a9575e79c5abdb75d75dd5a44993d4e2c58bd5134f3c51f5dc920e12ae00003668c5f9456fe3d33a1e97e50080e4917005000000d4c63c1900808cd327f387cbe5f271b2dc06059868f84956f387fee632d9ca28405bd654e4630f6363c2d5faf5ebd53efbeca3f6df7f7ff5d65b6fa9850b17aac99327abb163c7aadd77df5d5d7cf1c5de72950957e671ade54c5c7df5d5de364d22d741071da4060707bd727347ac891327aa1d77dcd14bc692cf2de920e10a006083569c5f66cd9ab5bbdece9bb21c00903c12ae00000080da982703009071aeeb7e5d9fd097ca721b1464a2e1275de536d9ca28485b062af2b187b121e12a6a3cf7dc735ec2d5f9e79f5f5597d520e10a006083569d5ff4763e2c954a1f91e500806491700500d128a5c60d0c0c2c79e8a187fe73d9b265de751b22d9d0affb9ffafbfbd7f6f4f47c59b60f00b40af364000032ae542afd993ea1bfedbaeec7645dda0a34d1d85e16e44d81dab24a918f3d8cb97021937eb2107d7d7dea5bdffa96977075f3cd3757d567354c7bc8360a43bf0600c4a155e717bd9ddfe9cf39536439002059245c01403403030377f6f7f7ab75ebd6a94d9b36555dc721e20ff3ba9bd7bfafafefbdeeeeee2fc936028056609e0c00400ee813fa2daeeb7e5b96a78d89467e14b92d8b7cec61b29a70f5d5af7ed5fbe9c0638e39c6fb0942599fd520e10a006083569d5ff4767ed5d1d171ac2c0700248b842b0088c6dcd9ca24fbc8eb3744f2313838b8bebbbbfb51d94600d00acc930100c801c7710ed727f5dfcaf2b431d1c88f22b765918f3d4c5613aef21a245c01006cd0aaf38bebbaffaab775b22c0700248b842b0088c6fc8c2077b6b2234c3b7477776f966d0400adc03c1900809cd027f5673b3a3a3e27cbd3c444233f8adc96453ef630245cd915245c01006cd0aaf38bebba17ea6d75c9720040b248b8028068b86e6657347add0c00eac53c1900809c705df7747d625f28cbd3c444233f8adc96453ef6305c38b22b1abd7044bf0600c4a155e717bd9dff69ee7225cb0100c922e10a00a269e4bad9e0db1fa879fff69417e6dfb29e683e1abd6e0600f5629e0c00404ecc9e3d7b17c771de29954a3bc9bab430d1c88f22b765918f3d4c23178e88f8a3d10b47f46b00401c5a757e715df70b7a5bbf92e500806491700500d1845d37dbbcf98feaadf73f1c7edcfffcebc30957e6df7ef91bef6e54ef6ef8afe588e8d1e8753300a817f364000072449fd8ef705df71bb23c2d4c34f2a3c86d59e4630f1376e188483e1abd7044bf0600c4a155e79752a934456feb77b21c00902c12ae00209a5ad7cd4cb2d54f1e7c499d7de7d3ea97cfac557dab06d5f94b9f1d4eb832ff36e53f7b728d3a4b2f73d57dbf55ef7d40d255b3d1e8753300a817f364000072a4a3a3e3587d727f4296a78589467e14b92d8b7cec616a5d3822d289462f1cd1af01007168d5f9a5bdbd7d7bbdad0f6539002059245c014034b5ae9bad7f6fa33a73c9d3c3095623c5fc3b56aa57d7bf5fb51da2b168f4ba1900d48b79320000f9325a9fdc071cc7f91b599106261af951e4b62cf2b187a975e18848271abd7044bf0600c4a195e717bdad374ba5d26eb21c00901c12ae00209ab0eb663f7feab5aac4aa5ab1e49197abd6271a8f46af9b0140bd982703009033fae47e9e8e1b64791a9868e44791dbb2c8c71e26ecc211917c347ae1887e0d0088432bcf2f7a5b4f94cbe54fc97200407248b8028068c2ae9b999f11f413aace58bc522dfacd6af587d7dff5c2fcdb94f9f5bf58f95ad5fa44e3d1e8753300a817f364000072a6542aeda54ff06fe9bf1f91754963a2911f456ecb221f7b98b00b4744f2d1e88523fa3500200ead3cbfe86ddda3e344590e00480e095700104dd075b3c1b73f50fdcfbfaece5ffaec70429549b092cb9932bfde2c6bd631ebcae588faa3d1eb6600502fe6c90000e4903ec1ff87e338b36579d29868e44791dbb2c8c71e26e8c211915e347ae1887e0d0088432bcf2ffaf3cc021da7c97200407248b802806882ae9bc99f0b3461ee6a259733657239137239a2fe68f4ba1900d48b7932000039e4baee09fa24df2fcb93c644233f8adc96453ef63041178e88f4a2d10b47f46b00401c5a797ed1db9aaf3fd7fc8b2c07002487842b008826e8ba994ca022e12ab968f4ba1900d48b7932000039d4d6d6b6ad3ec9af2d954a53645d929868e44791dbb2c8c71e26e8c211915e347ae1887e0d0088432bcf2faeeb76e85822cb0100c921e10a00a209ba6ec64f0aa6178d5e3703807a314f060020a7f449fe7baeeb5e2ecb93c444233f8adc96453ef63041178e88f4a2d10b47f46b00401c5a797e711ce770bdbd876539002039245c01403461d7cdfa56bd3e9c5075c6e2955e8295b9ab9509f36f53e6d79b65e5fa44e3d1e8753300a817f364000072aa5c2eefaf4ff4afcf9d3b77acac4b0a138dfc28725b16f9d8c32c5bb6ec4f9b366daaba8041241fba1dd67677776f966d14867e0d0088432bcf2fa552692fbdbdb5b21c00901c12ae00209ab084abfb9e7eadeae7026bc57f3cb9a66a7da2f120e10a405c982703009063fa44dfe738ce576479529868e44791dbb2c8c71ea6bfbf7fedba75ebaa2e6010c9c7cb2fbf7c477777f7a3b28dc2d0af01007168e5f9a5abab6b8cdedee652a9344ed601009241c2150044532be16afd7b1bd5d9773e5d9558552bbebd64a57ae3dd8d55db211a0b12ae00c4857932000039e638ce4c7db2ffb92c4f0a138dfc28725b16f9d8c3f4f4f47cb9afafefbdc1c1c1f5dce92a9dd0affbe0ead5ab17757777bfa2e34bb28dc2d0af01007168f5f9456fefa572b9fc09590e004806095700104dad84ab8d1f6e56dfef79d1fbd9c0bb1f7b45f5ae7a5d9dbff4d9e1042bf3efbe5583ea97cfacf512b3feb5ff0f6ad3e6cd55db211a0b12ae00c4857932000039d6d9d9395e9fecd7ebd85bd6258189467e14b92d8b7cec2331493ee6ce4a3afe682e5c10898779ddcdebdf50b29541bf0600c4a1d5e717d77597eb6db6c97200403248b8028068cc751b99f4e38749ba7afd9d0f861ff73ffffa70c295f9b75ffef6fb1f926cd5a230ed21db08005a817932000039a74ff6d7398ef31d599e04261af951e4b62cf2b123bfe8d7008038b4fafca2b7f7631db36439002019245c0140346109573206dffe6038e1cafc5bd613cd07095700e2c23c1900809c731ce7607dc25fddd5d53546d6c58d89467e14b92d8b7cecc82ffa3500200ead3ebfe8ed5da23fcf9c2bcb0100c920e10a00a26924e18a883f48b8021017e6c9000014803ee1af2897cbd36479dc9868e44791dbb2c8c78efca25f0300e2d0eaf38bdede3feab8519603009241c215004443c2955d41c21580b8304f0600a0005cd7fd077dd2bf5396c78d89467e14b92d8b7cecc82ffa3500200ead3ebf94cbe5e3f4367f21cb0100c920e10a00a221e1caae20e10a405c9827030050003367cedcd1719c774aa5d26eb22e4e4c34f2a3c86d59e463477ed1af01007168f5f9457f86f92bd7759f93e500806490700500d190706557907005202ecc93010028087dd2bfd575ddff2dcbe3c444233f8adc96453e76e417fd1a001087569f5fcc7f1cd1dbdc20cb0100c920e10a00b6e6baee3b3a4e97e51209577605095700e2c23c19008082e8e8e8f85b7de25f25cbe3c444233f8adc96453e76e417fd1a00108738ce2f7a9bef96cbe59d653900207e245c01c0d6cc786862a4c42b12aeec0a12ae00c4857932000005a24ffccf9bc42b591e17261af951e4b62cf2b123bfe8d7008038c4717e711ce769d7750f92e50080f8917005005bf313ae464abc22e1caae20e10a405c982703005020e62705f5c9ff56591e17261af951e4b62cf2b123bfe8d7008038c4717e711ce7673afe4e960300e247c215006c4d265cd54abc22e1caae20e10a405c982703005020a5526937c771de993973e68eb22e0e4c34f2a3c86d59e463477ed1af01007188e3fca2b7f97dd775bf21cb0100f123e10a00b62613ad64f88957245cd915245c01880bf36400000a469ffcefd41ffafe4196c78189467e14b92d8b7cecc82ffa3500200e719c5ff46797b3f576bf27cb0100f1f3c7759950401004418407095776856c9fac853c3f03b007ef5100000aa65c2e4fd3138015b23c0e4c34f2a3c86d59e463477ed1af01007188e3fce238ce4cbddd45b21c0010bf38c6750068a5a4c7299908e3073f29687764f90e5749f771008de13d0a0040c17475758dd11380971dc73958d6b51a138dfc28725b16f9d8915ff4eb78c90baf04d1aa907d0db04d1cfd546ff3b33a1e90e50080f8c531ae03402b253d4ec9cf6832d1ca47c2955d41c21580b8f01e0500a080f404e0021dd7c9f25663a2911f456ecb221f3bf28b7e1d2f5e5fc4817e852c88a39f96cbe58febedbe2ccb0100f18b635c0780564a7a9c1a29d1ca9756c2d5ba75ebcceba14e39e594e1b27befbdd72bbbf9e69bab960f8bf9f3e77beb0d0c0c54d5c9e8ebeb53b7df7e7b55b92d41c21580b8f01e0500a080f404606f1deb3b3b3bc7cbba5662a2911f456ecb221f3bf28b7e1d2f5e5fc4817e852c88a39fb6b5b56dabb7fbc752a9b48dac0300c42b8e711d005a29e9716aa4442b5f1e12aece38e38cba13ae8e3bee3875e289275695db12245c01880bef5100000a4a4f027ee138ce4c59de4a4c34f2a3c86d59e463477ed1afe3c5eb8b38d0af900571f553bddd353af696e5008078c535ae0340abd83a4ed99a70b56ad52aefdfe79d779e9a346992da79e79dd505175c30bcec39e79ca3264e9ca80e38e000d5dedebe55c2d569a79da6f6dc734f357efc7875e8a187aac71f7f7c781db39c1fb7de7aab577ed34d37a97df7dd576dbffdf6eaa8a38e524f3ef964d5f34d2a48b8021017dea300001494ebba5fd513815e59de4a4c34f2a3c86d59e463477ed1afe3c5eb8b38d0af900571f553bddddf747474fcad2c0700c42bae711d005ac5d671caf684abbdf7de5bfde8473f52c71c738cf7f8e1871f56cb972ff7fe7decb1c77a752659ca3cf613aeeeb9e71e75e38d37aa6bafbd56edb0c30e6afaf4e95eb9596fecd8b1eac8238f5477df7db77ae9a597d48a152bd4e8d1a3d509279ca06eb8e10675e08107aa430e39a4eaf92615245c01880bef5100000a6aeedcb963f544e0f552a9b49fac6b15261af951e4b62cf2b123bfe8d7f1e2f5451ce857c882b8faa9deee1dae26cb0100f18a6b5c078056b1759cb23de16adebc795eddfdf7dfef3dbeeebaebd495575ee9fdbbbfbfdfabbbf0c20bbdc726e1ca6cd724626dbbedb65e9989830f3e78781fdb6db7dd563f2978f9e5970f2fe7c7983163d4c68d1bab9e731241c21580b8f01e0500a0c0f444e00ad775bf2bcb5b8589467e14b92d8b7cecc82ffa75bc787d1107fa15b220ae7eaa3fb35caeb77da62c0700c42bae711d005ac5d6712aad842b93d0346edc3875fcf1c70f9799bb52e9a7a4eeb8e38ee1842bf3f380a6eebefbeef31e5f7ffdf5ea8a2baef0fefdc0030f7875975c7289f7d8245cf90954a79f7eba97bc64ee7e75d041070def43265c5d76d965def20b162c503d3d3dc3b179f3e6aae79c44907005202ebc47010028b052a934454f06d6b6b5b56d2beb5a8189467e14b92d8b7cecc82ffa75bc787d1107fa15b220ae7eea38cea97adbd7cb720040bce21ad701a0556c1da7d24ab832d1dedeeefdc4dfb9e79eebfdfcdf3efbeca376da6927353838389c70b5e79e7baadb6ebb4d1d7df4d1de4fff3df6d863aab7b7d7ab9b366d9afaf18f7facf6db6f3fefb149b8f213a8ce3cf34c2f31cbdce9aa32e16ad75d775593264d528b172f564f3cf184b73d7347ab238e3842dd7efbeddecf0a9a3b68c9e79a54907005202ebc47010028383d19e8771ce7cbb2bc159868e44791dbb2c8c78efca25fc78bd71771a05f210be2eaa7e6f38a8e7f97e5008078c535aea74129356e606060c9430f3df49fcb962df3be7c27920dfdbaffa9bfbf7f6d4f4f4f2cd721514cb68e53a6cfcba49fa462cd9a35aa5c2eabdd76db4d8d1f3f5e1d7ef8e16af9f2e55e9d9f7065ee806512a476d9651775e9a5970eaf6b12aa264c98e025539d77de79c30957669b871d769877272b939065ee9055997065ee643571e244b5e38e3baa458b1679650b172e5493274ff692bf76df7d7775f1c517573dd7a4c2b4876ca3acb0b58f03d882f728000005e738ce6c3d21b85796b702138dfc28725b16f9d8915ff4eb78f1fa220ef42b64415cfd546ff7af753c25cb0100f18a6b5c4fc3c0c0c09dfdfdfd6addba756ad3a64d555fc613f18779ddcdebdfd7d7f75e7777f797641b0151d83a4ea5997015167ec2d5f9e79f5f5597e720e10a405c788f02005070edededdbeb09c15ba552692f59d72c261af951e4b62cf2b123bfe8d7f1e2f5451ce857c882b8fae9ecd9b377d1db7e5b960300e215d7b89e0673672b93ec23bf8427928fc1c1c1f5dddddd8fca3602a2b0759cb23de1cadcbd4ad6e53948b8021017dea30000c04c086ed0719e2c6f16138dfc28725b16f9d8915ff4eb78f1fa220ef42b64419cfd546f7b63a954fa33590e00884f9ce37ad2cccf0872672b3bc2b4437777f766d9464014b68e53245cd915245c01880bef510000607e56f06ff4a46040ff73b4ac6b06138dfc28725b16f9d8915ff4eb78f1fa220ef42b64419cfd546ffbf952a974802c0700c427ce713d69b6263f1435b29cfc00bbd83a4e31e6d815591e736cede300b6e03d0a00003c7a52f0444747c7b1b2bc194c34f2a3c86d59e463477ed1afe3c5eb8b38d0af900571f653bdedfbcbe5f234590e00884f9ce37ad2ea4d7ef8e0dd4135b0e256f5dcaf2ef2c2fcdb94c9e588e622cbc90fb08bade354bd630e914c6479ccb1b58f03d882f7280000f0b8aefb0d3d31b84396378389467e14b92d8b7cecc82ffa75bc787d1107fa15b220ce7eaab77d8b8e53643900203e718eeb49ab27f961c33bafa967ef3f5fadfcd9195b852933757279227a6439f90176b1759caa67cc21928b2c8f39b6f671005bf01e0500009ececece098ee3bc337bf6ec5d645d544c34f2a3c86d59e463477ed1afe3c5eb8b38d0af900571f653fd59e53b7afbff2ccb0100f189735c4f5a3dc90faf3ef7ef55c9567eac79eedeaae589e891e5e407d8c5d671aa9e3187482eb23ce6d8dac7016cc17b1400000cd3138385aeeb9e2ecba362a2911f456ecb221f3bf28b7e1d2f5e5fc4817e852c88b39fea6d77eacf2a3f92e50080f8c439ae27ad9ee487e77b2fab4ab4f2c3d4c9e589e891e5e407d8c5d671aa9e3187482eb23ce6d8dac7016cc17b1400000cebe8e8f89c9e1c3c2bcba362a2911f456ecb221f3bf28b7e1d2f5e5fc4817e852c88b39fbaae7bb4de7eaf2c0700c427ce713d69f5243f3cdbdd559568e587a993cb13d123cbc90f16db5e161481ade3543d630e915c6479ccb1b58f03d882f7280000d88a9e1cfcd6719cc36579144c34f2a3c86d59e463477ed1afe3c5eb8b38d0af900571f6d352a9b49fdefe1f643900203e718eeb49ab27f98184abe422cbc90f969aaa63ddd0df42b1759caa67cc21928b2c8f39b6f671005bf01e0500005b715df7db7a82708b2c8f8289467e14b92d8b7cecc82ffa75bc787d1107fa15b220ce7e3a7dfaf4edf4f637eb7f8e9675008078c439ae27ad9ee487e77e755155a2951fa64e2e4f448f2c273f58c84fb69a3ff4b7504957b68e53f58c39447291e531c7d63e0e600bdea30000602baeeb7e4c4f10de2e954a7f26eb1ac544233f8adc96453e76e417fd3a5ebcbe8803fd0a5910773fd5db5fa73fa7fcb92c0700c423ee713d49f5243fbcf8e0f55589567e983ab93c113db29cfc60193fd9ca4fb2928f73cfd671aa9e3187482eb23ce6d8dac7016cc17b14000054d11384a5aeeb7e5d96378a89467e14b92d8b7cecc82ffa75bc787d1107fa15b220ee7eaab7bfa25c2e1f26cb0100f1887b5c4f523dc90f6fbdf6b47af6971754255b993253279727a24796931f2c522bb9aa56792ed93a4ed533e610c94596c71c5bfb38802d788f0200802ae572f9383d49785896378a89467e14b92d8b7cecc82ffa75bc787d1107fa15b220ee7eaab77f978ebf97e5008078c43dae27a9dee487971e5b58957065cae472447391e5e4074b8c945435527d6ed83a4ed53be610c94496c71c5bfb38802d788f0200802a5d5d5d63f424e155c771fe4ad63582894636e976eb356d3742f4caf5f28a7e8c3ca25fc78bd71771a05f210be2eea7faf3c9d53ae6c97200403ce21ed7935457f2c3e6cdeac5df7cbf2ae1ca9499baaae589c891e5e4070bd49b4c55ef729966eb3855d798432416591e736cede300b6e03d0a000002e949c245aeeb5e23cb1bc144239b74bbb589e4aaa06893ebe515fd187944bf8e17af2fe240bf4216c4dd4ff5f6bfd5ec67140040fde21ed7933452f2c386775e53bf7bf0baaa642b3f4c9d5946ae47448b2c273fa4acd124aa4697cf1c5bc7a991c61c22d9c8f298636b1f07b005ef51000010484f12f6d5f1e6f4e9d3b79375f562a2915d33c2ef72d52b97cf33fa31f2887e1d2f5e5fc4817e852c88bb9fbaaefb55bd8fbb643900201e718feb49aa99fcb079b31a7cb1473d73df3955495632cc326659ee76d57c6439f921455193a7a2ae9709b68e53cb962dfbd3a64d9baafa3e917ce87658abc79ccdb28db2c2d63e0e600bdea30000a0263d51f8a5e338336479bd986864d78cf0bb5cb5c9e5f38c7e8c3ca25fc78bd71771a05f210be2eea7e572f930bd8f15b21c00108fb8c7f52405255c8d7457ab5ac1ddae9a0f12ae1ad66cd254b3eb5bcbd671aabfbf7fedba75ebaafa3e917cbcfcf2cb77e831e751d94659616b1f07b005ef5100005093ebba1d7ab2f02b595e2f261ad93623f82e57bd72b9bca31f238fe8d7f1e2f5451ce857c882b8fb69a954fa73bd0ff36521002001718feb490a4ab8aae7ae56b5c2ac2bb747d41f245c35a455c952adda8e556c1da77a7a7abedcd7d7f7dee0e0e07aee74954ee8d77d70f5ead58bf478f38a8e2fc936ca0a5bfb38802d788f0200809a4aa5d2383d5978a35c2e7f42d6492611a72229a756142e5927cb6604dfe5aa4d2e9777e6b865199075f4eb78f1fa220ef42b644102fd74b4dec7a6cececef1b20200d07a098ceb89094ab89249548d86dc1e517f907055b7562749b57a7ba9b3799c32493ee6ce4a3afe68fa3c917898d7ddbcfe994db6326ceee300788f02008011b8aefb2f3a2e96e5528de41c196d723dd86dc6d68974854c9863c28c3ca25fc78bd71771a05f210b92e8a77a1fbf2f97cbfbcb720040eb2531ae27c57cf92e937e88f4c2b4876c235491c951a375cc19fadb08b99edc6ea6e5699c0282d0c701bbf11e050000a14aa5d2017ac2b046ffdd46d64922394746af5c1ef69bb175225d9bac2f0226ccc823fa75bc787d1107fa15b220897eeaba6e8fdecf31b21c00d07a498ceb4921e1caae20e16a443229ca244bfd508779ddccdf7a93ae6aad27b79f59791aa78020f471c06ebc470100c088f484e141c771fe4e964b223947469b5c1ed9306328914e961745918f1df945bf8e17af2fe240bf421624d14ff53e6ed3d129cb0100ad97c4b89e1412aeec0a12ae42052543cd19b52569ca8f7a92ae2a93adfc985d511fb49fccc9d3380504a18f0376e33d0a000046e438ced7f4a4e11e591ec44fce11d12b974376cc184aa493e54551e463477ed1afe3c5eb8b38d0af900549f453bd8f7fd69f4fbe23cb0100ad97c4b89e1412aeec0a12ae6aaa950415943c15967415b4fc2d43e5956aed2f33f2344e0141e8e380dd788f02008011cd9a35eba37ad2f0f649279db487ac93fce41c116d72b9a2504a8d1b181858f2d0430ffde7b265cbbc0b4a590cd38eb22c2ba15ff73ff5f7f7afede9e9f9b26c9f7a3061461ed1afe3c5eb8b38d0af900549f453bd8f5374982f0c0100314b625c6fd68c2dfff1af4d964be6fa804cfa21d20bd31eb28d2a9936356d2bcb6d66ae9fcab2068d94fc14944415947415b45c50b2956fa4fd5a2d0be314d00cfa386037dea30000a02e7ad270a3ebba67cbf2204317bbb8bb9536303070677f7fbf5ab76e9ddab46953d5052622fe30afbb79fdfbfafadeebeeeefe926ca3913061461ed1afe3c5eb8b38d0af900549f4d372b93c4defe797b21c00d07a498cebcdaabcfe342324f18a842bbba256c29569c3caeb8ab2de5643ff59f525d775a3262dd59bf414944c55997415541f966ce5ab77ffd6c9523f01a2a08f0376e33d0a0000eae238cea17ae2f0e2a8913fa0fb17470a7f772bc3dcd9ca24fbc80b4b44f2313838b8bebbbbfb51d9462361c28c3ca25fc78bd71771a05f210b92e8a7fa73c95fb8aefb822c0700b45e12e37ab32aae3f85265e91706557c884aba16b8995ff81335309578649b6d2cff98d0849578d263b05255599c76302caeb49b6f235fa3cac90b57e02348a3e0ed88df7280000a89b9e38acece8e8f8bc2c0fe25f2491e545637e46903b5bd911a61dbabbbb37cb361a09fd187944bf8e17af2fe240bf421624d14f87ee20b1519603005a2f8971bd597e624e406c957845c2955de1275c99361a6a2bd97e994bb83222245d454d720a4abafaad78dc48b2952feaf3494d16fb09d008fa386037dea30000a06eaeebfe939e3cdc2ecb830c5d3029fc44830b7a7685fc1f94f5a01f238fe8d7f1e2f5451ce857c882a4faa9decf5bb367cfde459603005a2ba971bd197e624e487889575c9fb12b162e5ce8b78d6cafad42b67716349074d56c725350d2951f5192ad7ccd3eaf4465b59f00f5a28f0376e33d0a0080859452e306060696989fa33377483217856c889ffdec67eae4934f56f7dd775f555d509889862ccb4ae8d7fd4ffdfdfd6b7b7a7abe2cdba711665bf2a25265acf8c39beacc254fab79fff654685cf1f317d4dab73654ad4f3416a63d641b8d840933f2c8d67e6debf9afd1e0fc8766cda8e3cb27b38c5c0f485b52e717bd9fa774fcb52c0700b456c0fc23b361e6baf21a01915ec8f6296ae8b7d97cf9be6b90f9194179672bf3d8943763be7cae36877cf2409ed0c701bbf11e0500c04203030377f6f7f7ab75ebd659f773741f7cf04155591ec3bceee6f5efebeb7bafbbbbfb4bb28dea157641efac3b9f568ffcfe0dd5fdecdaaa04aba0b8b1f7f755db201a0bd31eb28d46c2841979646bbfb6f9fc579468d5f90fcd993174a7d011a24dae07a42da9f38be338ffae83c45000405852586fe57c29ecfa0c917cf8d7674c1b0db5956c3f2f861b3a83eab8d395296fe64e526177b832e5dce10ac801fa386037dea3000058c8dcd9c37cd9292f4610c9c7e0e0e0faeeeeee47651bd52bec829e7fe7aa35eb3754255705c577963e53b50da2b120e10ad8c2d67ecdf9cf9e68f6fc87e6857df164eae4f2800d923abfe8fd5c6f7eee5c9603008a27689e342320313decfa0c917cc8eb33a6cd86da6eabf6ac5c268b624cba0a4ab69277ba8a927415f5f9a4260ffd0408431f07ecc67b1400000b999f51e2ce1e76846987eeeeeecdb28dea157641cf24519db178655539115fc80b7af560c28c3cb2b55f73feb3279a3dffa179435f3ac92f10fd6893cb033648eafce2baeeb7f5beae90e50080e2a9981f05265af9e4f519f31f3d74b13ae5945386cbeebdf75eafece69b6fae9a1f87c5fcf9f3bdf5060606aaea64f4f5f5a9db6fbfbdaa3cafb16ad52aefb539efbcf3b62aaf757d66680e3c9c7825ebb36828e96aa0bdbd7d7b5937a4d124a7a064ab5b466df9194159de48d255a3cfc30a79e927402df471c06ebc470100b090bc0844a41bb52e02d523ac2de51dacc242ae4b448b286dc9841979646bbf0e1b3389e423ca9889d6aafcb2a9227ae572802d923abf388ea3773563b12c070014cfd07ca94d964bf2b3462b13aece38e30c6fbd7a12ae8e3bee3875e289275695e7351a4db8f29936cdd3bc3724d9ca3775547dc94eb592adfca4aaa0fa7a92aeeaddbf75929a7f0269a18f0376e33d0a008085e445a0b0187cfb83e1a41cf36f594f341f235d040a13d69632a92a2ce4ba44b488d2964c989147b6f6ebb031b3323e7877500dacb8553df7ab8bbc30ff36657239a2b9883266a2b586be689209576d7239c016499d5ff47e8ed4f11b590e00402df2b3c648095795894293264d523befbcb3bae0820b86973de79c73d4c48913d501071ca0dadbdbbd65fd84abd34e3b4dedb9e79e6afcf8f1ead0430f558f3ffef8f03a66393f6ebdf556affca69b6e52fbeebbafda7efbedd5a98f473c00006b524944415451471da59e7cf2c9aab979d876c39e6b3d75d75d779d9a3c79b29a366d9a57be68d1223565ca146f3f9ffef4a7d5830f3ee8951f7cf0c16aefbdf7569b376ff61ecf9a354b6dbbedb6ead5575fadebb9551e0b9f35024d1d159ef414944c55996ce50b5a2e2ce96aa4fd5a2da9f9279016fa386037dea3000058485e04aa8ccd9bffa8de7affc3e1c7fdcfbf3e9c9463feed97bff1ee46f5ee86ff5a8e881ecd5c040a6b4b99541516725d225a44694b26ccc8235bfb75d898e9c786775e53cfde7fbe5af9b333b60a5366eae4f244f4883266a2f5666c7d97ab5e590fd824a9f38bdecfde3ad6c87200006a919f35ea4db83209463ffad18fd431c71ce33d7ef8e187d5f2e5cbbd7f1f7becb15e9d4996328ffd84ab7beeb947dd78e38deada6baf553becb0839a3e7dba576ed61b3b76ac3af2c823d5dd77dfad5e7ae925b562c50a357af46875c20927a81b6eb8411d78e081ea90430ea99a9b876d37ecb9d653f7d18f7e547df7bbdf550f3cf0807aeaa9a7bc24aa2f7ef18beac73ffeb1fae4273fa976dd7557f5ce3befa8abaebaca5bde1cc7860d1bd4840913bc64b37a9e1b0957759b3a2a38f92928892a28d9ca17b47c50d255adfd654652f34f202df471c06ebc470100b090bc08e48749b6fac9832fa9b3ef7c5afdf299b5aa6fd5a03a7fe9b3c34939e6dfa6fc674fae5167e965aebaefb7eabd0f48ba6a369ab90854ab2d4dc8a4aab090eb12d1224a5b3261461ed9daafc3c64c3f5e7deedfab92adfc58f3dcbd55cb13d123ca9889d69bb1f55daeda643d6093a4ce2fa552691bbdaf3fce9d3b77acac03002088fcac516fc2d5bc79f3bcbafbefbfdf7b6cee0675e595577affeeefeff7ea2ebcf042efb149b832db3589582669c99499307786f2f7b1dd76db6df59382975f7ef9f0727e8c1933466ddcb8b1eaf9d6da6ed873ada74e9f4f87f773f5d5577b657d7d7dde639340651e9bf5d6ae5dabc68d1ba74e3df554b574e952affcaebbeeaaebb99170d590a9a3aa93a0e68caae823a3c293ad7c414957b32bea83f6933949cd3f81b4d0c701bbf11e0500c042f222901febdfdba8ce5cf27455324ead987fc74af5eafaf7abb6433416cd5c04aad59626647b85855c97881651da920933f2c8d67e1d3666faf17cef655589567e983ab93c113da28c9988c78ca1bb5cc972c03649f653bdaf9775ec2bcb010008223f6b988426933c74fcf1c70f97993b34e945d51d77dc319c28647e2acfd4dd77df7ddee3ebafbf5e5d71c515debfcd1da14cdd25975ce23d3609577e02d5e9a79feecda7cdddaf0e3ae8a0e17dc884abcb2ebbcc5b7ec18205aaa7a76738fc9fedf3236cbb61cfb59ebaca6428ffd87a7b7bbdc7d75c73cd568fcd73df6bafbdd49c3973d4eebbefeebd8ef53c3712ae1a3675d4d6c95095c953f5245bf96aad27b79f5949ce3f8134d0c701bbf11e0500c042f2225065fcfca9d7aa92716ac592475eae5a9f683c9ab90814d696b2bdc242ae4b448b286dc9841979646bbf0e1b33fd78b6bbab2ad1ca0f53279727a247943113f1d0efd9b652a9447bc07a499e5ff4be7eddd1d1f139590e004090a0cf1ae6e7f0cc4ffc9d7beeb9de9d9cf6d9671fb5d34e3ba9c1c1c1e144a13df7dc53dd76db6deae8a38ff67efaefb1c71ef3928f4cddb469d3bc9fdddb6fbffdbcc726e1ca4fa03af3cc33bde42573d7a7ca842bf3f37c93264d528b172f564f3cf184b73d7347ab238e3842dd7efbeddecf0a9a3b68c9e71ab6ddb0e75a4f5d6532d4ca952bd536db6ca3bef0852f78c73665ca14f5b18f7d4cbdfdf6db5efd4f7ffa536f9ddd76db6df8ae59f53c3712ae22993aaa3ae9cadca1aade642b9f5c4f6e37d3929c7f0269a08f0376e33d0a008085822e02f9617e46d04fc23963f14ab5e837abd51f5e7fd70bf36f53e6d7ff62e56b55eb138d47331781c2da52265585855c97881651da920933f2c8d67e1d3666fa41c255721165cc446ca68e1f3fdeb4472ebe14417e25797ed1fbbadd75dd936439000041823e6bac59b34695cb652f79c8ccb50e3ffc70b57cf972afce4f143277c0320952bbecb28bbaf4d24b87d735c94513264cf0128b4c329159d6245c996d1e76d861de9dac4c4296b9b35465c295b993d5c48913d58e3beea8162d5ae4952d5cb8504d9e3cd94bfe32778dbaf8e28b039f6baded863dd77aea6432d44f7ef213f5894f7c427de4231ff1f6e9dfc9cbc4871f7ea8f6d8630f6f3d939c55ef7393fbe0b346ddccfcbf95c951adde5eea929c7f0269a08f0376e33d0a008085822e020dbefd81ea7ffe7575fed2678793704c82955cce94f9f56659b38e59572e47d41fcd5c040a6acbef2c7da62aa12a2cccf2721b44b488d2964c989147b6f6eba03153c673bfbaa82ad1ca0f53279727a247943113b1f0bf14993ff437375f8e207f923cbfb8aefb5d1d67cb72000082d4f359a332fc44a1f3cf3fbfaaceb6087bae617569069f351ae27f1e68f67340abb6639524e79f401ae8e380dd788f020060a1a08b403209c784b9ab955cce94c9e54cc8e588faa3998b40416d7963efefabda272cccf2721b44b488d2964c989147b6f6eba03153c68b0f5e5f9568e587a993cb13d123ca988996935f8ac8c78055923cbf388ef3bff4febe2fcb01000852cf678dcaa87567261b23ecb986d5a5197cd66858b39f039a5ddf5a49ce3f8134d0c701bbf11e0500c04241178164128e0912ae9289662e0205b5e5dab736a82b7efe42551b058559ce2c2fb741448b286dc9841979646bbf0e1a3365bcf5dad3ead95f5e50956c65ca4c9d5c9e881e51c64cb454ad2f456a9503a94bf2fce238cedfe9f8992c070020483d9f352ac3d644a5a0087bae617569069f352289fa3920ea7a9990e4fc1348037d1cb01bef5100002c147411889f144c2f9ab90814d496447a11a52d9930238f6cedd7f58e992f3db6b02ae1ca94c9e588e622ca98899619e94b9191ea815424797e715df720bdbf676439000041eafdac4124137cd688acd1cf018d2e9f3949ce3f8134d0c701bbf11e0500c042611781fa56bd3e9c5075c6e2955e8295b9ab9509f36f53e6d79b65e5fa44e3d1cc45a0b0b624928f286dc9841979646bbfae6bccdcbc59bdf89bef57255c99325357b53c1139a28c9968897abf14a977392031499e5f3a3b3b27e8fdbd27cb01000852d7670d22b1e0b34653eafd1c50ef729996e4fc1348037d1cb01bef5100002c147611e8bea75fabfad9b95af11f4faea95a9f683c9ab90814d69644f211a52d9930238f6cedd7238d991bde794dfdeec1ebaa92adfc30756619b91e112da28c99685aa35f8a34ba3c10aba4cf2faeebbe5f2a957692e5000048237dd620920d3e6b346da4cf0123d5e746d2f34f2069f471c06ebc470100b050ad8b40ebdfdba8cebef3e9aac4aa5af1ed252bd51bef6eacda0ed158347311a8565b12e94494b664c28c3cb2b55fd71c33376f56832ff6a867ee3ba72ac94a8659c62ccbddae9a8f2863269a12f54b91a8eb012d97f4f945efef59c771fe4a96030020d5fcac41a4127cd668895a9f036a95e752d2f34f2069f471c06ebc470100b050ad8b401b3fdcacbedff3a2f7b381773ff68aea5df5ba3a7fe9b3c30956e6df7dab06d52f9f59eb2566fd6bff1fd426be706e3a9ab90854ab2d8974224a5b3261461ed9daaf83c6cc91ee6a552bb8db55f31165cc4464cd7e29d2ecfa404b247d7ed1fbfb79b95c3e4e96030020057dd620d20b3e6bb48cfc1c201fe75ed2f34f2069f471c06ebc470100b050d845209374f5fa3b1f0c3fee7ffef5e1842bf36fbffcedf73f24d9aa45d1cc45a0b0b624928f286dc9841979646bbf0e1a33ebb9ab55ad30ebcaed11f54794311391b4ea4b91566d07882ce9f38bdedf8d3afe5196030020057dd620d20b3e6bb494ff3960fed0df427d1e487afe09248d3e0ed88df7280000166ae422d0e0db1f0c275c997fcb7aa2f968e62250236d49c41f51da920933f2c8d67e1d3466ca24aa46436e8fa83fa28c996858ab93a45abd3da021499f5f1cc73957eff352590e008014f45983482ff8acd15a53a64c396d681e56b8cf0149cf3f81a4d1c701bbf11e0500c0425c04b22b9ab908445bda1551da920933f2c8d67ecd986957441933d110991c355ac79ca1bf8d90ebc9ed028949fafca2f737cb75dd9fc8720000243e6bd8157cd6682d3d27ea4d7a1e668ba21e378a833e0ed88df728000016e222905dd1cc4520dad2ae88d2964c989147b6f66bc64cbb22ca9889bac9a428932cf5431de635377feb4dbaaab59edc3e9088a4cf2faeeb4ed5fbec97e50000487cd6b02bf8acd13a7a2ed466e66043d126ebf32ee9f9279034fa386037dea3000058888b407645331781684bbb224a5b3261461ed9daaf1933ed8a286326ea12940c3567d496a4293fea49baaa4cb6f26376457dd07e8058257d7e29954aff5deff325590e0080c4670dbb82cf1aad3363e8ee5643d12bebf32ee9f9279034fa386037dea3000058888b407645331781684bbb224a5b3261461ed9daaf1933ed8a28632646542b092a28792a2ce92a68f95b86ca2bd5da1f108ba4cf2f73e7ce1dabf7f9c7aeaeae31b20e00804a7cd6b02bf8acd11a33b6bebb5521ef7295f4fc13481a7d1cb01bef5100002cc44520bba2998b40b4a55d11a52d9930238f92eed733b6fc8fdb36592e3166da15238d994317f77b65396a1a29f92928892a28e92a68b9a0642bdf48fb055a26e9f38ba1f7b9b6542aed25cb0100a8c4670dbb62a4cf1aa8cfd0676d997055a8cf6869cc3f8124d1c701bbf11e0500c0425c04b22b9ab908445bda1551da920933f228e97e5d79e1774648e21563a65d516bcc346d5879615fd62350bd494f41c95495495741f561c956be7af70f34258d3141eff361d7758f90e5000054e2b3865d51ebb306ea37f4b94c265bf9d12697cfab34e69f4092e8e380dd788f020060212e02d915cd5c04a22ded8a286dc98419799474bf0eb8f81b9878c5986957c831d3b4d950db6dd59e95cb2050a3c94e414955e6b1f9c934595e4fb295afd1e701342c8d31c175dd257abf8e2c0700a0d2b265cbfeb469d3a6aa392f917ce87658ab3f6b6c966d84c6047d36ab885eb97c5ea531ff0492441f07ecc67b1400000bf185b35d21bf706e046d695744694b26ccc8a3a4fb75c0c5dfe18bc0332a12af1833ed0a7fcc346d34d456b2fdbc186e6804899ae4149474f55bf1b891642b5fd4e703d4258d31c1719c2b759c21cb0100a8d4dfdfbf76ddba7555735e22f978f9e597efd09f351e956d84fa0d7d46abfa6c26a24dae974769cc3f8124d1c701bbf11e0500c0427ce16c574449d2f1d196764594b664c28c3c4aba5f075cf895d16b2e063366da150b172ef4db46b6d75621db1bc39a4d6e0a4abaf2234ab295afd9e795a8f6f6f6ed6519ec95c698a0f7f94dc77116c87200002af5f4f47cb9afafefbdc1c1c1f58ddce9eafdf7dfaf2a23a2857edd0757af5ebd487fee7b45c797641ba17ef57c4e33cbc8f5f2288df9279024fa386037dea3000058882f9ced8a28493a3edad2ae88d2964c989147011762ad08c64cbb42b60fd178e8b7db7cf9fe6b90f9194179672bf3d8943763be7cae3687e33833e401c04ea6bd6459dc5cd73d41eff71e590e008064927ccc9d9574fcd17cf6088ac58b17ab050b16a873cf3d577df39bdf545ffffad7d52f7ef18baae5d20a73ae9565190af3ba9bd79f64ab18a4310fb341518f1bc5411f07ecc67b1400000b998b10f24b4f22bd30ed21dba85e616d69fe97e4bc79f3d49e7beea9c68f1faf0e38e000b564c992aae59a8dbebe3e75fbedb75795e73556ad5ae57d217dde79e755d545694b26cc40f364f24445f4cee02705ad0d7fcc346d34d456b2fdbc186e68485347357727a9b03b5c99f242dce1ca75dd83743ffb838e2b4aa5d236b21e7649634c2897cb9fd2fb7d429603003092e9d3a76fa7e71a47e9f3c8993aee751ce71d31d77d53c75fcbf5d294c6b916d950d4be51d4e34671d0c701bbf11e0500c0427ce16c574449d2f185b5a599888d1e3d5a9d76da69ea473ffa91fadad7bea67efdeb5f572dd76c1c77dc71eac4134fac2acf6b907005d8477c69519568e50b1b3389e4438e99a6cd86da8e84abfa454d6e0a4ab69277ba8a927415f5f9a4aa5c2eefec38ce7dbabffd6af6ecd9bbc87ad8238d31a1542aeda6f7bb5e960300508b3e6ffca38e5febd824e7b615f1c7af7ce52bff4dae9bb634ceb5c886a2f68da21e378a833e0ed88df7280000164aeb0be775ebd6795fe09d72ca29c365f7de7baf5776f3cd37572d1f16f3e7cff7d61b1818a8aa9361fb1d98e417ce8da8d5962fbef8a2f7fa74747454d5f90943d75d779d9a3c79b29a366d9a577ed34d37a97df7dd576dbffdf6eaa8a38e524f3ef9e4f03a2669cbbf53d6a1871eaa1e7ffc71affc9c73cef1bf94f5e2d65b6f1d715b95516bbb95494d93264d523befbcb3bae0820beaae93c7b668d1223565ca146f3f9ffef4a7d5830f3ee8951f7cf0c16aefbdf7569b376ff61ecf9a354b6dbbedb6ead5575fadebb9c9e389d2964c9881e6557c69119868e5ab3566b63a366edc383c2e6eb7dd76ea939ffc645de7b9356bd6a8091326a8cb2ebbacaaaede68e69cd7c8b9b515516bcc346d38d496245cd567eaa8c6929c8292ad6e19b5e5670465792349578d3e0fab7475758d715df7bbbacfbd34c3b23b4de0bfa43526e8fd7ed8dedebebd2c07002088e3387f117037abad42cf3bce95ebd920ad732dec57d4be51d4e34671d0c701bbf11e0500c042497de12ca3950957679c7186b75e3d5f0adb7e07a65a5f38d7a3565bde79e79ddeeb73edb5d756d5f909431ffde847d577bffb5df5c0030fa8152b567877c33ae18413d40d37dca00e3cf04075c821870caf73cf3df7a81b6fbcd1dbde0e3beca0a64f9fee952f5fbe5c8d1d3b561d79e491eaeebbef562fbdf4d288dbaa8c5adbf59fa349863277e73ae69863bcc70f3ffc705d7595c7f6d4534f7949545ffce217d58f7ffc632ff161d75d7755efbcf38ebaeaaaabbce5cd716cd8b0c14b74686f6fafebb9917005d863c6088956be5a6366abc34fb83209a726f9e988238ef0c6453f71332c7a7b7bd5e0e0605579bdd1cc39af91736b2b62a431d3b4a9695b598e405347d597ec542bd9ca4faa0aaaaf27e9aadefd5b4ff7b9bfd7f186ebba27c93aa42fad7993deef6f759ff8a42c0700a09672b93c4d9f3ffeaf397705c4c652a93451ae6383b4ceb5b05f51fb46518f1bc5411f07ecc67b1400000b25f585b38c9112aec2ee5c64c2dc4d69e2c489ea80030ef09262ccb2fe97c2b5ee46d4ec1d98928891be700e53ab2d972c59e21dafb9d393acf35fe7b973e70e975d7ef9e55bbd4e26c68c19e3250d98763bf6d863bda425bfcedc19ca5fd7dcbda5f2cbfdb06d553e8fb0edfacf71debc79dee3fbefbf7ff878eaa9ab3cb6abafbeda2b33777d318f4d0295796cd65bbb76ad1a376e9c3af5d453d5d2a54bbdf2bbeebaabaee7562be12ae042ea88310a40226a8d99ad0e3fe1aa542a798fcdb8621edf76db6ddee35ae72039bed45acec40f7ff843ef5c69c6e0fdf7df5fbdf0c20b91ce7961e7d6b8a399f31f024d1d159ef414944c55996ce50b5a2e2ce96aa4fd668eebba7fa9cfcfbf731ce7eab6b6b66d653dd293d6bc49f7896e1d5f90e5000084d1e7ad6fcacfff43f17db9ac2dd23ad7c27e45ed1b453d6e14077d1cb01bef5100002c94d417ce32ea4db80aba7391b90391f9b749823175e68b63f3d8ff52b8d6dd889abd035312d1cc17ceb5daf2e9a79ff65e9fcececeaa3af985be09f3f355a66cc18205aaa7a76738cc4fedf90954a79f7ebaf75ccd6b7fd041070daf2b13aec2b655f93cc2b6eb3f479348671edf77df7ddee3ebafbfbeaebaca63bbe28a2bbc3273d718f3f89a6baed9eab179ee7bedb5979a33678eda7df7ddbd44897a9e5bad84ab510d62c20c24a7d698d9eaf013aecc7966f5ead56ae6cc99de79c79c7fc2ce4195e34bd872a6ce24b24e9d3ad5bb8356575797dab46953c3e7bc91cead7147943113239a3a2a38f92928892a28d9ca17b47c50d255adfd659e9e434d701ce767fa3cdd5b2a957693f548475af326bddf1feaf89fb21c008091e8f3c70a916cf527f3938372395ba475ae85fd8ada378a7adc280efa386037dea300005828a92f9c65d49b701574e7a22bafbcd2fb777f7fbf5777e185177a8fcd97c261772332211382eabd035352d1cc17ce616df9b9cf7dce7b4dce3aeb2cb568d12275d1451779af5f50c2d0638f3de6bd06e667afcc97f7e64b79f39a9b3a3f81eacc33cff49297cc362b13aecccff399bbac2c5ebc583df1c413a1dbaa8cb0edfacfd1dcb5ccdc11e6e8a38ff61206ccb6eba9ab3cb6952b57aa6db6d9467de10b5ff07e5270ca9429ea631ffb987afbedb7bdfa9ffef4a7de3abbedb6db70dfabe7b9917005644fd898d9caf013ae2ac3247b9abab07350e5f812b69c5f679259e5be1b39e7859d5be576e388286326ea3275547512d49c515bf783b0642b5f50d2d5ec8afaa0fde44a5757d7187d9efe671d2febf8b4ac47f2d29a37e9fd76b9ae7ba12c0700a096cececef1fafcb1489f3f1ed17f1fa848b8fa855cd626699d6b61bfa2f68da21e378a833e0ed88df7280000164aea0b6719e6cb5df3f36dc71f7ffc7099b92b957e4aea8e3bee08bd73917f97a2071e78c0abbbe4924bbcc7e64be1b0bb1199905f3ed77b07a6a4a2992f9cc3dad2fc5c9eebbade4f4599d7c0241ad54ab832b170e1423579f264efee28e64e4f175f7cb157be66cd1a75d8618779db98366d9ad73e95afaf791dcd3e76dc71472fb12b6c5b9511b65dff399abe6292b976d9651775e9a597d65d278fed273ff989fac4273ea13ef2918f78fbf4fb91890f3ffc50edb1c71ede7a2639abdee726f761224a5b32610692133666b632fc84abcf7ffef3ea073ff881f7efabaebacaab0b3b07558e2f61cbf98952fe9dfa2aa391735ed8b9556e378e883266a26e53476d9d0c55993c554fb295afd67a72fbb9a6e75327e8f3f51bfaef1c598764a5356fd2fb3d59c7adb21c008020a552e9cff579c3245add6e12aff4e389fadf2f9af358b95c9e2697b7495ae75ad8afa87da3a8c78de2a08f0376e33d0a00808592fac23928dadbdbbd249c73cf3dd7fbf9bf7df6d947edb4d34e6a707030f4ce45e64b655367925fcc5d8af6db6f3fefb1f95238ec6e4426a2de8129a968e60be734db32cef0fbc2f9e79fdf505dda11a52d993003c9496accf413ae4aa592f7f8e4934ff612a11e79e491d0735065c255d8722641ca2cd7d6d6e625ba9a3b19bef2ca2b5e5d23e7bcb073ab3ca63822ca9889864c1d559d7465ee50556fb2954fae27b75b08e6a77f5cd77d419fb7af9b3b77ee58598f64a4356feae8e83856ef7b992c070040d2e78bbfd6f1b29e3b9c5b596ee612bafc37a31a9f8b252aad732dec57d4be51d4e34671d0c701bbf11e0500c042497de11c14e6ce41e572d9fbf9b6f1e3c7abc30f3f5c2d5fbedcab0bbb73910993503561c2042f99ca7c196d96355f0a87dd8dc844d43b302515cd7ce19c665bc6196177910aab4b3ba2b42513662039498d9932e16afdfaf5ff3f7b771f1f5579e77d9c204fa22020a262b156145c5d44eeaa55743515958d6ea2e29e5ce74c08a6dd967bbbebae96d5b5ab58a3abb688ebf3ba45da5551b2a2ab68b75b65b384905b5714149f6a6d6b81282684dbeab6ae42ef3f7addbfeb9049c335932133c9ccb9ce9ccffbf5fabe9239bfc9cc75e63c0d9e9fe7840dc6c71e7bacfed5af7ed5e731c8debff5f53c933befbc337c4d73ec33c73cd3b86ca6e77bccebebd86acf533152c83e13793b7bc8e036470df6ebc54a5d5ddd58396e3f23690b82e050bb8ee28bea7b93526a9abcf72fece90000f426df0f2e95e3c54e396eccb36b86fcfb607f7b9a6ba23ad6c27d495d37923adf480ed671c06d6ca3000038a854279cf38dcb572e2a660672c2d9d56539d0d84d07fdad459d4296255f9881d2717d9ff9939ffc2451c7c142f69928c86035490dd6ebc45d851cbb6f90bc27f9925d447145f5bdc99c2097f7de35c4f1ab920000a223c789c59276c92cbb1627511d6be1bea4ae1b499d6f2407eb38e036b65100001ce4ea0967971b698a99819c707675592635852c4bbe3003a5e3f23eb3b5b5557ff39bdf0c8f83cb972fcfa897630ad967a260036d961ae8df979d2008aae518de25f933bb86e289f27b93bcf7cefafafa49f6740040b23534348c926344936483e77987d9f5b889f2580bb72575dd48ea7c233958c701b7b18d0200e020574f38d370953f5797655253c8b2e40b33503a2eef332fbdf4d2f0967f73e6cc096f4168d7cb3185ec33312085364d15fa7765affb36736f4bfec9f3bc11761d832fcaef4df2deaf484eb6a7030092cb3458c9b1e125c94ad37865d7e328ca632ddc96d47523a9f38de4601d07dcc6360a0080835c3ee19cc40ce48433cbd2ad14b22cf9c20c940efb4cb752c83e1303966ff354becf4f9c9a9a9a31722c7f4af2c2fcf9f30fb7eb185c517e6f524aad96ccb3a7030092498e49b324ed726cb8ceaec55994c75ab82da9eb4652e71bc9c13a0eb88d6d1400000771c2d9ad0ce48433cbd2ad14b22cf9c20c940efb4cb752c83e1383a2bf4d54fd7d1e860ca930275be598be5d32db2e62f044f9bd49defbee2008aeb4a7030092478e0797ca7161673936e24679ac85db92ba6e2475be911cace380dbd84601007010279cddca404e38c761590ed6ad225b5b5bf5ca952b7b1e6fdfbe5d8f1b374e2f59b224e3b951a59065c91766a074e2b0cf4c520ad96762d0ecab996a5f7564a194aa92e37a97e4cfed1a064794df9b8220f81b79ff3bece90080649163c16249bb64965d2b07511e6be1b6a4ae1b499d6f2407eb38e036b65100001cc40967b7329013ce71589683d57075c10517e84b2eb964af69ebd6add39d9d9d19cf8d2a852c4bbe3003a513877d669252c83e1383aaafa6aabea6a31f3ccf3b468eed6f4abe5f555535d2ae6360a2fcdea494f2e4fd9fb0a7030092a1a1a161941c079a241be4787f985d2f17511e6be1b6a4ae1b499d6f2407eb38e036b65100001cc40967b7329013cec55a960f3cf0803eeaa8a3f4e8d1a3f599679ea9376fde1c4e9f3973a69e32658adebd7b77f8b8bebe5e0f1b364cbffffefbfa8a2baed093274fd6a3468dd2a79c728a7ee59557c2e7f46eb87af5d557c3df6fbef9e6b0f6ecb3cf868f972f5f1e3eeeeb35aebdf6daf079e93cf8e083198d5c4d4d4d7afaf4e9e1df9e7cf2c9fa85175ed8ebfdbffbddefea134e38411f78e0813a0802fdd9679f65ccf74053c8b2e40b33503ac5da6792c252c83e1383eeec217b3757d98f5100f97e74807cd778bcfb84ec11761d858bf27b93bcf797242fd9d30100e5cf345899638064a569bcb2ebe524ca632ddc96d47523a9f38de4601d07dcc6360a00808338e1ec560672c2b918cb72e3c68dbaa2a2425f7cf1c5fafefbefd73366ccd0279d745258bbe38e3bc2e6a5f5ebd7eb4f3ef924bca55f757575587bfae9a7f5b265cbf4dd77dfadc78c19a3abaaaac2e9f9345cf5f51ae6fd860f1fae67cf9ead57af5eadb76eddbad7ebbef6da6b61e3d7f9e79faf1f79e4117ddc71c7e9891327ea8f3ffeb8e779e6f1f7bfff7d3d6fdebcf0b1792f7bde079a4296255f9881d229c63e93149e42f699288ab387ec69b2baaafba7798c4120c7f86b241f48fec8aea130517e6f92f79e2ce9b4a70300ca9becfb6749da9552d7d9b57214e5b1166e4beaba91d4f94672b08e036e631b0500c0419c70762b0339e15c8c6579db6db7850d49bd3374e850fde9a79fea8e8e0e3d62c4087df9e597eba79e7a2aac3df9e493baabab4b9f7beeb961d353fa6fccd5b0ccebf5b7e12ad76b988c1c3972af5b0af67edd3befbc33fcbdb5b535ac99862df378cd9a353dcffbc637be11d6ba3f6f7ddf7df765ccfb4053c8b2e40b33503ac5d86792c253c83e13c5317dfaf42bcc152287d06c35e89452e7cbb17e87fcbcdcae217f517e6f6a6c6c1c2aefbfdbf3bc11760d00509e8220b854f6fd3be5383ecfae95ab288fb5705b52d78da4ce379283751c701bdb2800000ee284b35b19c809e7622ccb254b96840d49f7dc738f6e6969e949fa3682a6e9e988238ed0975d76999e346952d888956ed2baf2ca2bc3f931b7233cf1c413c3e7f76e8c32b72634bfdf74d34d616de5ca95e163d37095eb354c72355c2d5dba34fc7dddba7561edaebbeeea796cdf7ad0cc8b797cefbdf766ccfb4053c8b2e40b33503ac5d86792c253c83e13c521c7a2759ee7b13c8a443edb2fc867fc9ae4a172bf0d51b145fdbd49de7f8b526aaa3d1d00507e649fbf58d22e9965d7ca59d4c75ab8cbac1b84104208297dec633200008818279cddca404e381763596edab429bca2d5e9a79f1e364499db0ade7efbed3df5679e79266c583ae49043f4a2458bc269e926ad6baeb9266c7e3257a9cad670d5d9d919deaef08c33ce081b9fce3aebacb0661aae72bd8689b925e0d4a953f5aa55abc22b65f57eddd75f7f5defb7df7efabcf3ce0b6f29387dfa747de8a187ea8f3efa88862b003d8ab1cf2485a7907d26069f1c872a7bfd479c4abb8ec1515d5d3d5a3edf26c946c914bb8efe89fa7b93bc7f6b6d6ded97ede90080f2619aa3bb8fd91b3ccf3bccae97bba88fb5705752d78da4ce379283751c701bdb2800000ee284b35b19c809e7622dcb152b56e869d3a6e9e1c3878757b14adf02d0e4b3cf3ed3871f7e78d8b4641a9dccb4eddbb7eb534f3d35bc0ad5dcb973f515575c91b5e1ca3cbee5965bf4f8f1e3c3862d33cdbc8769b8caf51a26e68a5b13264cd063c78ed54d4d4d19affbe8a38feaa38f3e5aefbffffee1eb3cfffcf359df9f862b20b98ab5cf2485a5907d26069f1c87d675375b99acb3eb185c4110fc8d7cce1d3ecd6d0589fa7b93bcff0aa5d4027b3a00a03c98062bd9d7bf245999d4ab52467dac85bb92ba6e2475be911cace380dbd84601007010279cddca404e38b32cdd4a21cb922fcc40e9b0cf742b85ec3331b84cd34f77a355ef54dacfc3e00a82e01cf99c3be5e795760db945fdbd4996d9cd3286c5f6740040fcc9fe7d96a45d29759d5d4b92a88fb5705752d78da4ce379283751c701bdb2800000ee284b35b19c8096796a55b296459f28519281df6996ea5907d260697bff7d5adb8ca5509a552a9cfcb67fd8ae411cff3f6b7ebc82eeaef4d4110fc6f19c303f6740040bcc9fefd52d9bfef544acdb36b4913f5b116ee4aeaba91d4f94672b08e036e631b0500c0419c70762b0339e1ccb2742b852c4bbe3003a5c33ed3ad14b2cfc4e0f1b35fdd8aab5c959069b492cf7a85e455d38065d79129eaef4d4aa92ac973f67400407cc9b165b1a45d32cbae2551d4c75ab82ba9eb4652e71bc9c13a0eb88d6d14000007ad5dbbf677bb76edca38f1494a1f590e1dcdcdcdbbed65d45f340fb895429a07f8c20c940ec73f7732d0e31f06cecf7e752bae721501a5d415f299774ae6d835ec2deaef4d41109c206378db9e0e00889f86868651b24f6f926cf03cef30bb9e54511f6be1aea4ae1b499d6f2407eb38e036b65100001cd4d6d6d6d1d5d59571f293943eededed8f353737bf6c2fa3fea2e1caadd07005b88de39f3b19e8f10f03e3e7beba1557b98a40100467cb67de21b9caaee1f7a2fede5453533346c6f0893d1d00102fa6c14af6e72f49569ac62bbb9e64511f6be1aea4ae1b499d6f2407eb38e036b65100001cd4d2d272516b6bebaf3b3b3b3fe44a1fd1443ef7ce6ddbb635353737bf27b9d05e46fd45c3955ba1e10a701bc7bfe83358c73f0c8c9ffbea56e9acb3ff0ec5259ff914c9c62008fea5baba7ab45d871bdf9b94521fa752a9f1f67400403cc8b16496a45df6e7d7d935b871ac859b92ba6e2475be911cace380dbd8460100709439c969ae2c21f9ad691221258ff9dccde73fa093cde6b5ec93d924ba98e5612fa37de10b33505a66bfdbbdffe5f8174d06e5f887c1c7f1c80d555555238320f867591eafa752a9a3ed7ad2b9b09e9a65a3949a694f0700b84f8eb197ca7e7ca7ecc7e7d935ece1c2b1166e4aeaba91d4f94672b08e036e631b050000282273f2da6efa21d1c52c0f7b19ed0b5f9801002ee078e416591e7f29d9914aa5e6dab52473613d9531fc2808826a7b3a00c06db2ff5e2c6997ccb26bf83d178eb5705352d78da4ce379283751c701bdb2800004011d170e55668b80200c415c723f7044170a62c970f9452dfb26b49e5c27a2a63b85ff297f67400809b1a1a1a46c97ebb49b2c1f3bcc3ec3af6e6c2b1166e4aeaba91d4f94672b08e036e631b05000028221aaedc0a0d570080b8e278e426cff38e302788254fc8ef07daf5a471613d350d709225f67400807b4c83951c3b5e92ac348d57761d995c38d6c24d495d37923adf480ed671c06d6ca30000004544c3955ba1e10a0010571c8fdce579de08593e0f48de92df8fb1eb49e2c27a2a63480541f02ff67400805b647f3d4bd2ae94baceaea16f2e1c6be1a6a4ae1b499d6f2407eb38e036b65100008022a2e1caadd0700500882b8e47ee9365b450d2954aa52eb06b49e1c27ada7dabc7e7ede9000077c8befa52d957ef544acdb36bc8cd85632ddc94d47523a9f38de4601d07dcc6360a00005044345cb9151aae000071c5f1281e8220385d96d5fb92c5f2b0c2ae973b17d653cff38e34cbc09e0e00708339464ada25b3ec1af6cd85632ddc94d47523a9f38de4601d07dcc6360a00005044345cb9151aae000071c5f1283e3ccf3b4c96d70b4aa9d535353563ec7a3973613d95cfff2019c7ef8220a8b66b0080e83434348c92fd739364833956da75f48f0bc75ab829a9eb4652e71bc9c13a0eb88d6d140000a08868b8722b345c0100e28ae351bc2c5cb870b82cb3fb256f7b9e37ddae972b57d6d3bababad3642c1d41107ccdae01004aafbb19f925c94ad37865d7d17fae1c6be19ea4ae1b499d6f2407eb38e036b65100008022a2e1caadd0700500882b8e47f124cbedab922e498d5d2b472eada79ee71d23e3f945100437da350040e9c8be7896a45d29759d5d43fe5c3ad6c22d495d37923adf480ed671c06d6ca30000004544c3955ba1e10a0010571c8fe22b954a9d2acbef3d49a33cacb0ebe5c4b5f5d4f3bc43644c2f4b7e50595939ccae03008a2b08824b651fbc532935cfaea130ae1d6be18ea4ae1baeceb7d67ac4962d5b1e7ff1c517ffdfdab56bc3ff26494a1bf9dc7fd7d6d6d6d1d2d27291bd7ce2c4d5751cc01e6ca30000004564fe716737fd90e8629687bd8cf6852fcc000017703c8ab7fafafa494110ac574afdd0f3bc83ec7ab970713d95cffe0019d78f243f36bfdb75004071c87e77b1a45d32cbaea1702e1e6be186a4ae1baecef7962d5b9e686b6bd35d5d5d7ad7ae5d19ff8d92143fe673379f7f6b6bebaf9b9b9b2fb497515cb8ba8e03d8836d140000a088ccff49c33faadd882c870ef9c7f56e7b19ed0b5f9801002ee078147fe60a4b4aa97b6459fe4c7efe815d2f07aeaea79ee7ed279ff97219df46d3fc66d7010083a7a1a16194ec6f9b241b64ff7b985dc7c0b87aac45f492ba6eb83adfe6ca56a6d9c7feef93a4f4e9ececfcb0b9b9f9657b19c585abeb38803dd8460100008ac85cb6987f5cbb91f6f6f6c70af9c7355f9801002ee078543e94520b6479ee945c62d7e2cef5f554c67783e4dd542a75ac5d03000c9c69b092fdec4b9295a6f1caae63e05c3fd6223a495d375c9d6f731b41fe275c3762964321ff13ae2b5c5dc701ecc1360a00005044e61ef1e6b2c5e6ffa4e11fd9d1443ef7ce6ddbb635c93facdf2be4f2d17c610600b880e3517949a5525f9465ba2d08829b1b1b1b87daf538aaaeae1e6dd653c99fda3597c8f8be2ae9947cc9ae01000a27fbd5599276a5d475760d8387ef84e84b52d70d57e7bbb9b939e3bf5192e8629687bd8ce2c2d5751cc01e6ca300000045669a7ccc959524bf35ffb823258ff9dccde79f77b395c1176600800b381e951fcff30e0982a04596ed8f1b1a1ac6d9f5384aa55227994632c90df2b0c2aebb42c679818c71a77cfed5760d00903fd99f5e6af6ab4aa979760d838bef84e84b52d70d57e7dbfc3749bbe9a77736fef2ffea6b1e7f432ffa97d77266e98fdfd11dbffa24e3ef497e31cbc35e4671e1ea3a0e600fb6510000002007be3003005cc0f1a83c5556560e93657b87e4174aa93fb4eb71545f5f3f49e6e705c913e6aa5776dd1532be93251f4816da350040ffc97e74b1a45d32cbae61f0f19d107d49eabae1ea7ce76ab8fad6136fe897dedda99bdfeac868b0ca9665ebdecd780d925f68b802502c6ca3000000400e7c610600b880e35179534ad5f97bae0ce2d9b538f23c6f84cccf8392572553ecba2b52a9d4d132be9f4bfedeae0100726b68681825fbcf26c906d9ef1f66d7511c7c27445f92ba6eb83adfb91aaed257aedafee12719cd55d9f2eda7decc780d925f68b802502c6ca3000000400e7c610600b880e351f9ebbe1ddf16c9771b1b1b87daf538524a2df2f75c456ab65d73451004134db380e44173c531bb0e00c8641aac64bff99264a569bcb2eb281ebe13a22f495d375c9def7d355c5dbdeaf58ce9a478a1e10a40b1b08d0200000039f0851900e0028e47c9b060c1828383206896e5bdc6f3bc09763d8e647efe58e6a74bd260d75c616e7da894faa18cf159f9dc0fb4eb0080df937de52c49bbec37afb36b283ebe13a22f495d375c9def7d355cf537f6df92c242c3158062611b0500000072e00b3300c0051c8f92c3f3bcfd8220b84d96f92f955233ed7a1cc9fc1c27f3f373f9f90fae5ebdcb7cee32c665924d32ce43ed3a0020dc9f5feaefb905ee3cbb86d2e03b21fa92d475c3d5f9a6e1caadd07005a058d846010000801cf8c20c007001c7a3e49165aecc49ed40d8b5384aa552e3657efe43f263cff30eb2ebae90f15defef69769b66d70020c964dfb858d22e9965d7503a7c27445f92ba6eb83adf345cb9151aae00140bdb2800000090035f9801002ee078944c7575753364d9bfab94badd5c81c9aec74df755a4ee96fc34954a1d6bd75d219ff757648c9df2f334bb060049d3d0d0304af6894d920db21f3fccaea3b4f84e88be2475dd7075be69b8722b345c012816b6510000002007be3003005cc0f128b9baaf0cf5ac646d100413ed7a1cc9bc7c5db2a3b6b6f65cbbe60af9acffd8df73dbac8bec1a00248569b0927de14b9295a6f1caaea3f4f84e88be2475dd7075be69b8722b345c012816b6510000002007be3003005cc0f128d91a1b1b87ca3a70ab64ab5f26b77292f9f823494710047f65d75c914aa5be2863fc40f2e7760d00ca9d39de48da9552d7d9354487ef84e84b52d70d57e79b862bb742c3158062611b0500000072e00b3300c0051c8f60044170a9ac0b3b25f5762d8e643e8e524abd213f1f58b870e170bbee02cff3be20e3fb99e416bb0600e52a7dbc917df43cbb8668f19d70f069ad476cd9b2e5f1175f7cf1ffad5dbb366ccc8863ccba614f8b4be473ff5d5b5b5b474b4b4bde571675759b30f36537fda4633755e58afdb7a4b098e5612fa3b870751d07b007db2800000090035f9801002ee07884b420084e90f5e1e7f2f3aecacaca61763d6e3ccf3b5029b55ae667bdabb74c34e392cffc45f9f9b0ab8d61003058647fb758d2ee97c91515cb0ddf0907df962d5b9e686b6bd35d5d5d7ad7ae5d198d1aa4f8319fbbf9fc5b5b5b7fdddcdc7ca1bd8c7271759bc8d670f5eda7decc68a8ca15f37cfb354861a1e10a40b1b08d0200000039f0851900e0028e47e8cdf3bc83649df891a4b5bebe7e925d8fa18a20086e96f9d952575737c32eba403ef3fd657ccf48d6d4d4d48cb1eb0010770d0d0da3641fd724d920fbbcc3ec3adcc077c2c167ae6c659a7dec060d52fa7476767ed8dcdcfcb2bd8c7271759bc8d670b56cddbb194d55b9629e6fbf06292c345c012816b6510000002007be3003005cc0f1085954c87a7193bfe72a2427dbc538524ac9ac84b7b0cafb7632a5e079de7e32be7f92bc4a3302807262f669b26f7b49b2d2345ed975b883ef8483cfdc46902b5bb911b31c9a9b9b77dbcb281757b7896c0d571dbffa442ffdf13b198d55d9629e679e6fbf06292c345c012816b6510000002007f385991042087121f6310a304c7392ac1f3b250d762d8e643e4e96bc27b9d6aeb9423ef3eb647c5b3ccf9b6ed700206e647f364bd26ef66d760deee13be1e0cbd61843a24bbe8d31ae6e13ac576e25dff5ca25aeaee300f6601b05000000000000622c0882e324eff8be7fdfc2850b87dbf5b8993f7ffee1322f1b244de6567e76dd05f2795f26e3eb94ccb66b001017b22fbbd4df7365c179760d6ee2a4dee0db5763ccc65ffe5f7dcde36f645c81c80e57241a9ce4db18e3ea36b1aff58a9436f9ae572e71751d07b007db280000000000001073757575637ddf7f5af27fcae17677e69656322f8f4836cafc1c61d75da0943a5fc6d71504c1c5760d005c27fbafc5fe9edbd2ceb26b701727f5065faec6986f3df1867ee9dd9dbaf9ad8e8c06ab6c59b6eedd8cd720f925dfc61857b7895ceb15297df25daf5ce2ea3a0e600fb651000000000000a03c5428a5beedfbfefb922fd9c53892f9b846b23d954a9d6ad75c601a15ccf88220f80bbb06002eea6e686d926c288706dda4e1a4dee0cbd51893be72d5f60f3fc968aeca966f3ff566c66b90fc926f638cabdb44aef58a943ef9ae572e71751d07b007db28000000000000504694527fe2efb9f2d2d7ec5a1ca5e7477ed6d93517c8d88e32b774947c471e56d87500708569b0927dd64b9295a6f1caaec37d9cd41b7cb91a634c13d5d5ab5ecf984e8a977c1b635cdd2672ad57a4f4c977bd7289abeb38803dd8460100000000008032934aa58e0d82e027beef2ff33c6f845d8f1b999713645ede957cb7b1b171a85d8fda82050b0e96b1bd207964e1c285c3ed3a0044cddf7345be76a5d475760df1c149bdc197ab31c6be8255aed87f4b0a4bbe8d31ae6e13b9d62b52fae4bb5eb9c4d5751cc01e6ca30000000000004019f23cef40dff79f94fc9764b25d8f1bd3d41404418b52ea8735353563ec7ad4e4f3de5fc6b65ac6d8ece2f880b8d05a8fd8b265cbe32fbef8e2ff5bbb766d7892940c2ccf3efbacfedad7bea6972f5f9e51cb16f9dc7fd7d6d6d6d1d2d27291bd7c503a72ec5e674ee2ed23ebecbf437ecc3a6f3767a4633755e58afdb7a4b098e5612fa35c4a7da2bb7bbbacb4a7db72ad57a4f4c977bd7249a9d77100f9611b05000000000000ca5785effbd74ab64b66dbc5b8a9acac1c26f371bfe4ad542a75b45d8f9ab9fa9652ea1f259be7cf9f7fb85d07b06f5bb66c79a2adad4d777575e95dbb76659c342585e537bff94dc6b4be623e77f3f9b7b6b6febab9b9f9427b19a1344c534796062b3b95f6df213fb91a63eca6aa5cb1ff9614967c1b634a7da2bbd7b697b3f12ad77a454a9f7cd72b97947a1d07901fb651000000000000a0cc0541f0c7beefef504a7dc3aec591cccf5fc8fc74e63ad11525f99cbf2563db2ae33cceae01c8cd5cd9ca34fbd8274b49e9d3d9d9f9617373f3cbf63242e9743775d84d563d0d1ff6f391bf5c8d31765355aed87f4b0a4bbe8d31a53ed19d6d3bf4b37c1fcdb55e91d227dff5ca25a55ec701e4876d140000000000004800a5d454c91bbeefffa0aaaa6aa45d8f9bdadada2fcbbcec90fcb95d73818cabde8c4fc679865d03d037731b41ae6ce546cc72686e6ede6d2f23948e69e4c8d2e0914ea5fd7ce42f57638cdd54952bf6df92c2926f634ca94f7467d90eb3365ee55aaf48e993ef7ae59252afe300f2c3360a00000000000024447d7dfd01beefaf92bc545757f739bb1e37a6892c08829f98dbf899db0ddaf5a8c9d8ce93cfba4bc637cfae01c88e93d46e25ce27a9cb45772347467387fd3c1426d73ec76eaaca15fb6f4961c9779f53ea13dd59b6453b61e355aef58a943ef9ae572e29f53a0e203f6ca300000000000040c20441f0b7beef7f505b5b7b965d8b9bbababab1322f3f92acf53c6f825d8f5a2a953a49c6b65d2975b95d0390299f93d49d1ffd4f4fb383f9ddae938127ce27a9cb8569dec8d2d451693f0f85c9b5cfb19baa72c5fe5b5258f2dde764d9369c48aef58a943ef9ae572e31eb933d0d803bd8460100000000008004eabefad20ef9f957762d6e1a1b1b872aa596c8fcbceb79def1763d6aa954eaf332b69f9a31cac30abb0ee0f7729da4debdfbb7fa57bff9ace771db4f77f4343b98dfd3d377fef7a7fabf3ff9fdf348e189f349ea72e2ef7d952bae6e3588b2ed73befdd49b190d55b9629e6fbf06292caeef73ecc6aadedba5cf2d059d8debeb552e3473006e631b0500000000000012caf3bc2ff8beff5a10040f3734348cb2eb7123f3522fe9925c68d7a266aebe25e37a5e3eeb47e5f711761dc01e7d9da436cd568fbeb055ffdd136fe8ff78b343b7bedda9af7feaad9e8607f3bb99feef9bb7eb6fc973ee78ee67fad7ff43d3d54013e793d4e5c43472f46aeca8b4eb285cb67dceb275ef663455e58a79befd1aa4b0b8becfb19aac321aadd2b2ad5724bab8be5ee5423307e036b65100000000000020c1aaabab47fbbebf52b2c9f3bc23ed7adcc87c7cc9df730bbfabed5ad44c539b8ced49c97f9a5b21da75007d9fa4fef0d79fea6b1e7f23a3d1a1af5cf5d8ebfafd0f7f93f13a24bfc4f92475b9e96eec60790cb26cfb9c8e5f7da297fef89d8cfd4ab698e799e7dbaf410a8bebfb9c7d355aa5655baf72e5d34f3f35f31d66e4c891fab8e38ed3cb972fcf789e9deddbb7eb71e3c6e9254b9664d4fa9bd6d656bd72e5ca8ce98399abaeba2a9cb72d5bb664d4ec14633caeaf57b9b0df07dcc6360a0000000000006088526a91effb1db5b5b55fb66b71e379de11a6814cb2a2aaaa6aa45d8f92b9fda18ceb5ec96b92c9761d48ba5c27a97ffcda0719cd0e7de5f197da33fe9ee49f389fa42e37a6b9438e6f2c8f41966b9f434a1fd7f739fb6ab44acb77bd4a375c9d79e69961b3d1e9a79fae2b2a2af42bafbc92f15c3bebd6add39d9d9d19d3fb9b0b2eb8405f72c92519d30733575f7d75bf1bae8a311ed7d7ab5c68e600dcc6360a000000000000201404c139beef774abe69d7e2c6f3bcfd653e1e93bc28bf1f66d7a3269ff5dfcad8b6c9d88eb76b4092e53a496d6e23986ea8ba7ad5ebbae9bfb6e95feef8ef30e677332d5d7ff6f50f32fe9ee49f389fa42e43678f1a35ca2c8fb3ed020a976b9f434a9f72d9e7e4bb5ea51bae4c53a579fce4934f868f1f7ae8a1f0f1030f3ca08f3aea283d7af4e8b0296bf3e6cde1f4b7df7e3b7cdee2c58b733ecfe4073ff8819e3a756a7805ad638f3d56bff3ce3bfada6baf0dff3e9d071f7c70af71bdfaeaabe1f49b6fbe397cfcecb3cf868fcdd5b77abfb779ddf1e3c7eb1b6eb8a1e76fcd6b4f9830411f7ffcf1bababa3a7c6ebae1ea8a2baed093274fd6669f76ca29a7f43496f5359e5cf3d59fc479bda29903701bdb28000000000000801ee6b682beefbf1204c1a3a669c9aec78dcccb62c97ba954ea8b762d6a4aa93a19db0ec91fd93520a9b29da4eefce87f74db4f77e8eb9f7aaba7a1ca3458d9cf33d3d275f35cf337e66fede791fe27ce27a9cbccd9922ec955dd3fcd630c826cfb1c125dca659f93ef7a956eb8baf8e28bf5b66ddb745d5d5d7885ab8d1b378631bf9bdafdf7dfaf67cc98a14f3ae9a4f0ef7a373de57a9ea90d1d3a549f7df6d9e115b41a1b1bf5ae5dbbf4faf5ebf5f0e1c3f5ecd9b3f5ead5abf5d6ad5bf71a577f1aaea64c99a21f7ef8613d67ce9cf0f1860d1bc2d735bf9f7beeb961cd344b99c7e986aba79f7e5a2f5bb64cdf7df7dd7acc9831baaaaa2a9c9e6d3cb9e6abbf89f37a453307e036b651000000000000007b696868181504c1c34aa9cdbeef1f65d7e346e6e112c94e89b26b519331cd917449fed4ae014994ed24b57dbb401373552bfb79669afd3c13fb79a4ff89f349ea32926eb64a3759d98f3100d9f63924ba94cb3e27dff52add70d53b77dd755758bbedb6db326aa679cafc4def86ab5ccf4bd75a5a5a32dedb5cf1aaaf5bf8f5a7e16ad1a245616dcd9a35e1e37befbd57df7efbede1ef6d6d6d61edc61b6f0c1f9b86abaeaeaeb0116bd8b0613de39c3973669fe3c9355ff678fb4a9cd72b9a3900b7b18d02000000000000c8caf7fdbf9674d6d6d69e6bd7e226088213655eb64afe5e1e56d8f52829a566cab8de379fb75d039226db496abb818a86abd225ce27a9cbc4d943b23757f5351d79cab6cf21d1a55cf639f9ae57e986ab2f7ff9cbfa7bdffb5ef8fb1d77dc11d6962c59123ebee79e7bc286a97476efdebd57c355aee7a51ba0d6ad5b97f1de768353ef985bf799bfbbe9a69bc2c7e6ea58e671ef862b737b40537beeb9e7c2c7f7dd779f5eba7469f8fbf3cf3f1fd66eb9e596f0b169b84a37505d79e595e1f23657bf3af1c413fb1c4faef9b2c7db57e2bc5ed1cc01b88d6d14000000000000409f6a6b6bcff27dbf432975b55d8b1bcff30e917969933c555f5f7f805d8f52f7ad1cdf962c1de2584318504ad94e52734bc1e812e793d465e0ec21b99baaf655473f64dbe790e8522efb9c7cd7ab74c3957c1f0c1f7ff5ab5f0d1b8f5e7ae925bd69d3a6f08a4ea79f7e7ad8f0646eab671aa8ccf37a375ce57a9e697c32cfabacacd44d4d4dfa5bdffa967eefbdf7c2dac48913f5d4a953f5aa55abc22b5af51e57676767783bbf33ce38236c723aebacb3c2d7e9dd703579f264fdd0430fe973ce39277cae198769ec32b5b973e7ea471e79441f73cc31e163d370956ea0bae69a6bc2c62c73a5abde0d57f67872cd577f13e7f58a660ec06d6ca30000000000000072aaababfb9ceffb2f4b1e73ad51295f9ee78d504a2d9779793d954a7ddeae4749c633bebb21acc98cd3ae034990eb2475ebdb3b7a1aaaae5ef57ad86065ae6a65627e37d3d275f35cfbef49fe89f349ea98eb6f33557f9f873ee4dae790d2a75cf639f9ae5776c3d5871f7ea88f3cf2487decb1c7ea5ffdea577ac58a157adab4697af8f0e17ad2a4493db7f8ebdd70651ef7f53c933befbc337c4dd3c8651a9c4c3395996eae1c3561c2043d76ecd8b019cb1e9bb93ad5f8f1e3f521871c12be8f79edde0d573535356183d4c1071fac6fbdf5d69ebf330d55e3c68d0bdfcbfc9d79ae69b8dabe7dbb3ef5d453c37198862c7385acde0d57d9c6936bbefa9338af573473006e631b05000000000000b04f555555237ddfff8152ea8d542a75b45d8f1b998f2bcc95bb822038d3ae45a9a1a161948ceb0919578be77907d975a0dce53a49fddc1b1f64dc2eb0affc68f3f68cbf27f927ce27a9632cdf26aa7c9f8f5e72ed7348e9532efb9c52ad573ff9c94fc246a6ebafbf3ea356eca41baea278ef7c13e7f58a660ec06d6ca300000000000000fa4d29f50ddff777a452a9b9762d6e8220384fe6a54bf267762d4a8d8d8d43654c779be636cff38eb0eb4039ebeb24f587bffe54ffdd136f643456f595bf7dfc75bdf3bf3fcd781d925fe27c923aa60a6d9e2af4ef12afaf7d0e8926e5b2cf29c57ad5dadaaabff9cd6f864d4fe68a5376bdd8b1afaee572e2bc5ed1cc01b88d6d14000000000000405e6a6b6bcff07d7f7b10047f67d7e24629354de6e31dc95d9ee7ed67d7a3249ff155927619db09760d28577d9da4fef4b3ddfa9f5a7e11de3670f5a6f7f4bab777e8eb9f7aaba7c1cafcdefa76a7fe8f373bc2c6ac7f6efba5deb57b77c6eb90fc12e793d43134d0a6a981fe7d22f5b5cf21d1a45cf639a558af2ebdf4d2f0167b73e6cc096f4168d78b1d1aae4a83660ec06d6ca300000000000000f2e6fbfe64c98b4aa97ff53cef40bb1e27e6d67d321fcfc9fcac6968681867d7a314087345b1dadadab3ec1a508e729da4364d573b3efe9f9ec76d3fddd1d370657e4f4fffe8379fd16c354889f349ea9819ac66a9c17a9dc4c8b5cf21a54fb9ec7358afdc4a9cd72b9a3900b7b18d020000000000002888e779237cdf5f26792b954a1d6bd7e3c45cdd4ae6e30ec9cfe4f7e9763d4ab5b5b55f9671750541506bd7807293cf49eace8ffea7a7e1cafc6ed7c9c013e793d43132d84d5283fd7a652d9f7d0e297eca659fc37ae556e2bc5ed1cc01b88d6d14000000000000c080f8beff75d31024b9d0aec58d52ea2b665e52a9d45cbb16a5bababa1932aef78220b8d2ae01e58493d46e25ce27a963c26e8eaa905cd6fd331ff6dfd9af8b3eb0cf712be5b2cf61bd722b715eaf68e600dcc6360a00000000000060c09452a7f9beffbee4fa21f99f28764a6d6ded19321f1dae3537c998a648de9271fdc390987fc6405f3849ed56e27c923a06eca628b35fff81c47ce6e6677ff7f37dfd9dfdfac8827d8e5b29977d0eeb955b89f37a453307e036b6510000000000000083c2f3bcc37cdf7f5ef2744d4dcd18bb1e27322f472aa536cbbcfcc0dc3ad1ae47a5a1a1615c1004eb655c8f5555558db4eb40dc7192daadc4f924b5e3b235435d36644fd3543afd69baeadd6c95ce825ef56cef835ed8e7b89572d9e7b05eb99538af573473006e631b050000000000003068162e5c385c29f58fbeefffd4f3bce9763d4eeaebeb0f9079f957d34426bf4fb2eb51318d5641103c2ee35a279ff141761d88334e52bb95389fa476585f4d50d99aa772355d657bfef7bba7f7d6d7fb6108fb1cd7522efb1cd62bb712e7f58a660ec06d6ca300000000000000069d52ea2bbeef77c9cf8bec5acc54044170a3cccb3699979976314215329e3b655c6fd6d5d57dce2e0271c5496ab7b2af93d4b20faa34cd9ff674f4695fcd4fd99aa8b2355d657b5eb666abb47dbd6fc8341adbd3e2caac9766fdb4a7dbd8e7b8957ded73e282f5caadc479bda29903701bdb2800000000000080a2504a9de2fb7ebb69581ad2f749e0589079f1645e76cacf79762d4a329e4532aef7e4e71fda35208e3849ed56fa3a499d6eb432279938d1d46ffd6a7a1a92bd99aa77d355b67aae66abb49cef6f9aad64596e956376d67adca4d7cd7d355ed9fb9caeaeaef033fdfad7bfde33eddffeeddfc269cb972fcfd84672e5aaabae0aff6ecb962d19353badadad7ae5ca9519d3cb356fbffd76f8d92c5ebc78afe97ded73e2c65eaf48b489f37ac53116701bdb2800000000000080a231b7e20b8260bdeffbff16f7dbdfc93ccc320d644aa96fdbb528c99894a42bd70965202e3849ed56ec93d4663fd3ddc0926e66a1e1aa7f72363b6591ada9ca3c1e9a657a7f9aadd2728ec3345bc9f2dc590e4d57f63adabdde56dacfb3f73983d97075f5d557877fd79f86ab0b2eb8405f72c92519d3cb35345c915226ceeb15c758c06d6ca3000000000000008aaab2b27298effb774b7ee679def1763d4e82203854e6e3bf24abaaabab47dbf5a89893c8fe9ea62b65d78038e124b55b499fa4eedec764345aa5632f47ec256793530ed99aae7e663dcea7d92a2de778caa5e9ca5e477b65afc62b7b9fb3af86abde8d4253a74ed5e3c78fd737dc7043cf73afbdf65a3d61c2047dfcf1c76bf99e103e37dd7075c51557e8c99327eb51a346e9534e3945bff2ca2b3d7f639e97ce830f3e184e7fe08107f451471da5478f1eadcf3cf34cbd79f3e68c6d34d7ebe61a6b7f6af7de7baf9e366d9a9e3b776e38bda9a9494f9f3e3d7c9f934f3e59bff0c20be1f4993367ea2953a6e8ddbb77878febebebf5b061c3f4fbefbfdfafb1f59e973837c6f466af5724dac479bde2180bb88d6d140000000000004049f8be5f6f4ee2ba765bbe7c5555558d94f97848f24a5d5ddde7ec7a54cc6d05fd3db7175c64d780b8e024b55b59b16245ba41c56e5ad92bf672448fb387e4686eea876c4d57e914d26c9596735ce5d07465afa359b24e5269ef73fadb70651a8c1e7ef8613d67ce9cf0f1860d1bf4faf5ebc3dfcf3df7dcb0669aa5cce374c3d5d34f3fad972d5ba6efbefb6e3d66cc182ddf27c2e9e6ef860f1fae67cf9ead57af5eadb76eddaa376edca82b2a2af4c5175facefbfff7e3d63c60c7dd24927656ca3b95e37d758fb533be08003f477bef31dfdfcf3cfebd75e7b2d6ca23afffcf3f5238f3ca28f3bee383d71e244fdf1c71feb3beeb8237cbe998f4f3ef9448f1b372e6c36ebcfd868b822a5489cd72b8eb180dbd84601000000000000948c52ea7ff9bebf4d724b6363a3b935526c0541f037321f1fc8cfd3ed5a544c03988ce94df99cef1c52f8897820329ca4762b599a54489e91d5fa2a7b3dcf933956da57b6328f077a0cbdca1e6b52d37b9def6fc3d5a2458bc2da9a356bc2c7e66a50b7df7e7bf87b5b5b5b58bbf1c61bc3c7a6e1cabcae69c4324d4b669a89b93254fa3d468e1cb9d72d056fbbedb6decb3bccd0a143f5a79f7e9a31debe5e37d758fb535bb87061cffbdc79e79de1b4d6d6d6f0b169a0328fcddf757474e8112346e8cb2fbf5c3ff5d453e1f4279f7cb25f63a3e18a9422715eafcc3eca9e06c01d6ca3000000000000004a2a088289beefaf953c9b4aa5c6dbf538514a55c97c74c9cf05762d2a9ee71d24635a279ff3e3e66a5c761d701927a9dd0ab7141cb0b387e4b892543fe4bac295995e68636dbfc615e72b5dd9eb68af98f5b832fd3c7b9f631a9a4cf3504d4d4dcf3473852679aa7eecb1c77a1a85ccadf24cedb9e79e0b1fdf77df7d7ae9d2a5e1efe68a50a676cb2db7848f4dc355ba81eaca2baf0cb72b73f5ab134f3cb1e73dec86ab254b9684cfbfe79e7b744b4b4b4fd2b7ed4b27d7ebe61a6b7f6abd9ba1d2f3b66eddbaf0f15d77ddb5d76333f6238e38425f76d9657ad2a449e1e7d89fb1d170454a9138af571c6301b7b18d020000000000002839cff3f60b82e01f7cdf7fb7aeae6e865d8f13a5d41fc87cfc42b2d495ab7675dff6f031f98cd73734348cb3eb80ab3849ed56ec93d4a651a5bb618586abfe3b7b483f9a9bb2c8d66c655fe9aa90a6abbcc613d7a62b7b1ded5e6f2bede765dbe798dbe1995bfc5d77dd75e1959c8e3cf2487dd04107e9cecece9e46a1c99327eb871e7a489f73ce39e1adff366dda14361f99dadcb973c3dbee1d73cc31e163d370956ea0bae69a6bc2e62573d5a7de0d57e6f67c53a74ed5ab56add2afbefa6af87ae68a56a79f7eba5eb97265785b4173052d7bacb95e37d758fb53ebdd0cf5faebafebfdf6db4f9f77de79e1bc4d9f3e5d1f7ae8a1faa38f3e0aebcf3cf34cf837871c7248cf55b3fa33b6726db85abb76edef76edda95b1bc48e923cba143d6abddf6328a0b8eb180dbd8460100000000000044c6f7fd54f789dc5abb1627e64a5d321fffa994faf7bababab1763d2215dd4d6d6f49a6d845c045d99a1f4a917ddd46cc7e7eae5c75d5553d4d1676cd8eb93d9769a6b0a7bb92be9a1f4ce34a77030b0d57fd73f6903c9a9c86646fb6fafe903db711b4a7e7d37495ef3842dd4d575baaabab47db3557edabd12a2ddb3e67fbf6ed5a8eeb61f3d0a851a3f469a79da6d7af5f1fd6d28d42e60a58a641eae0830fd6b7de7a6bcfdf9ae6a271e3c6858d45a699c83cd7ec0bcc6b9e7aeaa9e195ac4c4396b9b254ef862b7325ab091326e8b163c7eaa6a6a670da8a152bf4b469d3c2e62f73d5a89b6fbe39eb58fb7add5c63ed4fcd6e867af4d147f5d1471fadf7df7ffff03dd357f232f9ecb3cff4e1871f1efe9d69ceeaefd8ecf7e86b9f13376d6d6d1de6b8d27bde4834696f6f7f4cd6ab97ed6514171c6301b7b18d020000000000008894526aa639912b3f97982b5fd9f5b8a8acac1c26f371afe46d998f63ec7a548220b852c6f45edcaf248664c8d6fc508a0c66c3d5d5575fddd36461d7ec5c70c1057bdd46ccb5ecabf921dd78654f4756a6c9a93fcd4e7d355ba59baab2d5fbd374d5dff7cf2a4ecd56c6be1aadd2f2dde7a41b85aebffefa8c9a6bc935d65cb528b3af7d4e5cb4b4b45cd4dadafaebcecece0fb9d2553491cfbd73dbb66d4db24ebd27b9d05e4671413307e036b6510000000000000091f33c6f82effbff61627eb7eb7122f3b050b24332c7ae45c55c414cc6d3253fcfb16b804bf26d7e18acecabe1aaf7d558ccd560c68f1faf6fb8e1869ee75e7bedb5e1d5698e3ffef8f07664e6b9e9862b73551773db2e73a59c534e3945bff2ca2b3d7f639e97ce830f3e184e7fe08107f451471da5478f1eadcf3cf34cbd79f3e68cf1962ae5d2fce0907d353d656ba6eadd6c9596ed79b99aaef6f5be8995ef3ea7af2b33b9985c63cd558b32e5b4cf314d3ee6ca4a92df9af98a634c23813d2d46319fbbf9fc63db6c65d0cc01b88d6d140000000000008013ccd5adcc55aefc3d57bb9a69d7e3a4fbf64b9d92bfb46b51a9adad3dcb3482c9d802bb06b8c29c24b54fc09722fd6db89a32658a7ef8e187f59c3973c2c71b366c086f35667e3ff7dc73c39a6996328fd30d574f3ffdb45eb66c99befbeebbf59831637455555538ddfc9db955d8ecd9b3f5ead5abf5d6ad5bf5c68d1b75454585bef8e28bf5fdf7dfaf67cc98a14f3ae9a48cf1962a6679f45e3e18147d353f656ba2cad66c9596edf9d99aaefa7a3f0cc97f9fe36aa352b6e41a6bae5a94619fe3161a09a2c73200dcc6360a000000000000c029dd5763da2949d9b538f13cef0b320f6f2aa5beb770e1c2e1763d0af2d99e20636a975c65d70017e4dbfc3058e96fc3d5a2458bc2da9a356bc2c7f7de7bafbefdf6dbc3dfdbdadac2da8d37de183e360d57e6754d23d6b061c3c269263367ceec798f912347ee754bc1db6ebbade779e90c1d3a547ffae9a719632e45687e289ab3876436415d3664ef659fabd92a2d5bd3d5825ef56cef835ea2dae790ec619fe3161a09a2c73200dcc6360a000000000000c039757575337cdf7f3708827f3057beb2eb7121633f50e6e31949abcccb44bb1e0519d3114aa937644c773736360eb5eb4094a26a7e300d4d23468cd03535353dd3cc55a96448fab1c71eeb69b832b70734b5e79e7b2e7c7cdf7df7e9a54b9786bf3ffffcf361ed965b6e091f9b86ab7403d595575e19361298ab5f9d78e2893def61375c2d59b2247cfe3df7dca35b5a5a7ab27bf7ee8c319722343f1495dd0cd5bb79aa3fcd56697dfd9dfdfac822aa7d0ec91ef6396ea191207a2c03c06d6ca3000000000000009c944aa5c6fbbeffac64ad2bcd4a05aa9079b855f24ba5d41fdac528789e77907ca62d329e7f6d68681865d781a844d9fc505d5d1ddee2efbaebae0b6fff77e49147ea830e3a48777676f6345c4d9e3c593ff4d043fa9c73ce096ffdb769d326bd6eddbab03677ee5cfdc8238fe8638e39a6a7e12add4075cd35d7848d59e64a57bd1bae264e9ca8a74e9daa57ad5aa55f7df5d5f0f5cc15ad4e3ffd74bd72e5caf0b682e60a5af6584b159a1f8ace6e8a32cd52e60a55fd6db64ab3ffce7e5df421ca7d0ec90cfb1cb7d048103d9601e036b65100000000000000ce325760f27dff16c936a5d4ffb2eb7122f390f2f7dc2ab1c6ae45c1f3bc11329626499b696eb3eb4014a26c7ed8be7dbb966d411f72c8217ad4a851fab4d34ed3ebd7af0f6be9862b73052cd32075f0c107eb5b6fbdb5e76f4d43d5b871e3c266aac58b17f7345c99d73cf5d453c32b5999862c7385acde0d57e64a5613264cd063c78ed54d4d4de1b4152b56e869d3a685cd5f93264dd237df7c73c6584b159a1f4a62b09ba306fbf5ca5a94fb1c9219f6396ea191207a2c03c06d6ca3000000000000009ca7949a679a95e4a7b982476cc9f84f91f9783f0882bfb36b113157df5a2a79dbf3bc23ed22506aae363fa41baeaebffefa8c5a3987e6879219ac26a9c17a9dc470759fd33be9fd8f69e6b46bf9a4b5b535bc725efab16908358da2e64a7cf673a30afb1cb7d048103d9601e036b65100000000000000b1e079def1beefff4c72776565e530bb1e1732fec99297252b5db99d9f8ce5af4d2398526aa65d034ac9d5e687c16a78885b687e28a981364b0df4ef13c9d57d4eef0cd6fee7820b2ed0975c72c95ed3cc2d51cd6d53ede74615f6396ea191207a2c03c06d6ca30000000000000062c3f3bc837cdfffb72008d6d7d7d74fb2eb71611aad4cc35577e3d564bb1e0519c79f4aba2473ec1a502aae363f0c56c343dc42f343c915da3455e8df255eb1f6390f3cf0803eeaa8a3f4e8d1a3f599679ea9376fde1c4e9f3973a69e32658adebd7b77f858becbe861c386e9f7df7f3fbce5e8e4c993c35b9a9e72ca29fa95575e099fd37bfff3eaabaf86bfa76f35faecb3cf868f972f5f1e3eeeeb35aebdf6daf079e93cf8e08319fb35735bd3e9d3a7877f7bf2c927eb175e7861aff7ffee77bfab4f38e1047de081076af91ea63ffbecb38cf91e68d8e7b8854682e8b10c00b7b18d02000000000000889b8a20086ef47dbfdddca2cf2ec6898cff5bdd579672623e642c7f24d921e3a9b36b402914abf98114169a1f22916ff354becf472fc5d8e76cdcb851575454e88b2fbe58df7ffffd7ac68c19faa4934e0a6b77dc7147d8bcb47efd7afdc9279f84b7f4abaeae0e6b4f3ffdb45eb66c99befbeebbf598316374555555383d9f86abbe5ec3bcdff0e1c3f5ecd9b3f5ead5abf5d6ad5bf77addd75e7b2d6cfc3afffcf3f5238f3ca28f3bee383d71e244fdf1c71ff73ccf3cfefef7bfafe7cd9b173e36ef65cffb40c33ec72d3412448f6500b88d6d14000000000000402cf9be5f63aec8a494fa8a5d8b93eef9d82949d9b52874dfba715b10047f6bd780622b46f303293c343f44a6bf4d54fd7d1efa508c7dce6db7dd163624f5ced0a143f5a79f7eaa3b3a3af4881123f4e5975fae9f7aeaa9b0f6e4934feaaeae2e7deeb9e7864d4fe9bf3157c332afd7df86ab5caf613272e4c8bd6e29d8fb75efbcf3cef0f7d6d6d6b0661ab6cce3356bd6f43cef1bdff84658ebde2fe8fbeebb2f63de071af6396ea191207a2c03c06d6ca30000000000000062cbf3bce9beefff5429f58f0b172e1c6ed7e342c6ff87321fbf94dc2a0f2bec7aa9c938264b5e93dcdbd8d838d4ae03c5528ce6075278687e88d4d943723753edab8e7e28c63e67c992256143d23df7dca35b5a5a7a92be8da0697a3ae28823f465975da6274d9a143662a59bb4aebcf2ca70bb33b7233cf1c413c3e7f76e8c32b72634bfdf74d34d616de5ca95e163d37095eb354c72355c2d5dba34fc7dddba7561edaebbeeea796cdf7ad0cc8b797cefbdf766ccfb40c33ec72d3412448f6500b88d6d1400000000000040acd5d4d48cf17dff69c9f39ee71d66d7e362c1820507cb3cb44a9e91f938d0ae975a5d5ddd5819cb7f4a9e6c68681865d781622846f303293c343f44eeec21d99baafa9a8e3c15639fb369d3a6f08a56a79f7e7ad810656e2b78fbedb7f7d49f79e699b061e990430ed18b162d0aa7a59bb4aeb9e69ab0f9c95ca52a5bc35567676778bbc233ce38236c7c3aebacb3c29a69b8caf51a26e6968053a74ed5ab56ad0aaf94d5fb755f7ffd75bddf7efbe9f3ce3b2fbca5e0f4e9d3f5a1871eaa3ffae8231aae128c4682e8b10c00b7b18d02000000000000280715beef5f2f795f29759a5d8c0b73952e19fff7643edef43cef0b76bdd4640c23822078b4bb996d825d07065b319a1f48e1a1f9c109767395fd180350ac7dce8a152bf4b469d3f4f0e1c3c3ab58a56f0168f2d9679fe9c30f3f3c6c5a328d4e66daf6eddbf5a9a79e1a5e856aeedcb9fa8a2baec8da70651edf72cb2d7afcf8f161c3969966dec3345ce57a0d1373c5ad091326e8b163c7eaa6a6a68cd77df4d147f5d1471fadf7df7ffff0759e7ffef9acef4fc35572d048103d9601e036b6510000000000000065c3f7fd0b255d9285762d4e64fc7f29e90c82c08513ea154aa925329e9fa652a9cfdb45603015abf98114169a1f9c916eb2baaafba70bc786b2c03ec7adb0cf710b8d04d16319006e631b05000000000000505652a9d4b1beefbf2579c05ca1c9aec7858c7f8e64872bcd634aa9cb652cdbe5f33dc9ae018385e607b742f38353d24d57345b0d22f6396e857d8e5b6824881ecb00701bdb2800000000000080b2e379de814aa97ff57dff45c964bb1e17321fc7c8f8df96dc5b595939ccae979a7ca6f3642c5d41109c67d780c140f3835ba1f9c139a3ed091818f6396e857d8e5b6824881ecb00701bdb2800000000000080b21504c1df99ab32d5d6d69e61d7e2a2aeae6eac52eadf653efe33954a8db7eba5663ecbee2b6fd5db3560a0687e702b343fa0dcb1cf712bec73dc422341f4580680dbd8460100000000000094b5542a35d734080541f017762d2e1a1b1b87ca3c2c95fc4229f50776bdd4e4b33c4ec6b2d534b4d9356020687e702b343fa0dcb1cf712bec73dc422341f4580680dbd8460100000000000094bd542a75b452ea8d2008feb9aaaa6aa45d8f0b998705beef77c9cf2abb566af3e7cf3f5cc6b159f28fa621ccae0385a0f9c1add0fc8072c73ec7adb0cf710b8d04d16319006e631b050000000000009008f5f5f507f8beff98e4e5bababacfd9f5b8504a9d26f3f081e42abb566a353535638220689631adf63c6f7fbb0ee48be607b742f303ca1dfb1cb7c23ec72d3412448f6500b88d6d1400000000000040a228a5aef67dbfa3b6b6f62cbb1617a6614ce6e115c943515fb16be1c285c3651c2b242f2c58b0e060bb0ee483e607b742f303ca1dfb1cb7c23ec72d3412448f6500b88d6d1400000000000040e2d4d6d69eebfb7ea7e4afed5a5c5457578f96f1af92fc57100487daf512ab90317c47f28e8ce728bb08f4d7dab56b7fb76bd7ae8c93f0a4f491e5d0d1dcdcbcdb5e464039a1e1caadd070e5161a09a2c73200dcc6360a00000000000020914c6390526ab3b93a534343c328bb1e1732feeb25ed925976add48220f80b19c77617c682786a6b6bebe8eaeaca38094f4a9ff6f6f6c79a9b9b5fb69711504e68b8722b345cb9854682e8b10c00b7b18d02000000000000482ccff3f60f82e051dff75f91df8fb4eb71a1949a27f3b0537e7a76add4e4f3bc58c6d2256339dfae01fbd2d2d272516b6bebaf3b3b3b3fe44a57d1443ef7ce6ddbb635353737bf27b9d05e464039a1e1caadd070e5161a09a2c73200dcc6360a00000000000020f17cdfffa6a433088273ec5a5c28a566ca3c6c9379b8511e56d8f5529271cceefe3c2fb36bc0be98261f736525c96fcdc97752f298cfdd7cfe345ba1ec9975de6efa21d1c52c0f7b19213a3412448f6500b88d6d1400000000000000446d6ded977ddfef504a2db26b71515f5f3f49e6e17999877f95df0fb0eba5e479de7419cb1619cb75760d000017d070e55668b8720b8d04d16319006e631b05000000000000806ee6b682beef6f92acacaeae1e6dd7e340e661848cff074aa9cd51df2651deff3019cbab927f92dff7b3eb00004489862bb742c3955b6824881ecb00701bdb2800000000000000f4d2d0d030caf7fd8724af799ef705bb1e1741105c29f3d0515b5b7b865d2ba59a9a9a31328e359267e4f3dcdfae030010151aaedc0a0d576ea191207a2c03c06d6ca300000000000000904510047fe5fbfe0ef9799e5d8b8b542a3557e6a14b29f515bb564a0b172e1c2e9fe3c3329617e5e744bb0e00401468b8722b345cb9854682e8b10c00b7b18d02000000000000401f7cdfff23c9074110fcad5d8b0bcff3a6cb3cfc4c7247d4b7f59331dc62c612e72b870100ca070d576e85862bb7d048103d9601e036b65100000000000000c8a1aeaeee73beefbf2459555f5f7f805d8f838686867132fe354aa9e73ccf3bc8ae97928ce3cf4d135b2a95faa25d0300a09468b8722b345cb9854682e8b10c00b7b18d02000000000000c03e5455558df47dfffb4aa9372453ed7a1c98ab5b05417097e41d99876976bd94e4fd2f92cf73a78ce58fed1a0000a5b276eddadfeddab52ba3f187943eb21c3a9a9b9b77dbcb08d1a191207a2c03c06d6ca300000000000000d04fdd5767da11e7462119ff9f49ba641eceb36ba5a4943a4dc6d1293fbf62d700002885b6b6b68eaeaeae8ce61f52fab4b7b73fd6dcdcfcb2bd8c101d1a09a2c73200dcc6360a0000000000000079f07d7fb664bbe45a795861d7e32008823365fc1d4aa92bec5a29992b6dc9387e29b9deae0100506c2d2d2d17b5b6b6febab3b3f343ae74154de473efdcb66d5b537373f37b920bed6584e8d048103d9601e036b65100000000000000c8d3fcf9f30ff77dffbf244f7a9e77a05d8f83542af57919ffeb4aa9e5320f23ec7aa9044170a88c63936499b9eda15d0700a0984c938fb9b292e4b7124d4a1ef3b99bcf9f662bc7d048103d9601e036b6510000000000000028806952524a7d2f08829fa452a963ed7a1cd4d7d71fe0fbfe539236999f43ec7aa998a63519c3b3f279feb0baba7ab45d0700004069d148103d9601e036b6510000000000000018802008bee6fb7e9752ea4fec5a4c54c8f86f926c957939d12e964a6565e53019c383920d328e89761d000000a5432341f4580680dbd8460100000000000060807cdfff92e47da5d4b7e561855d8f8320086a651e764a2eb16ba524effff7929fcb6739d5ae010000a0346824881ecb00701bdb28000000000000000c8220080ef57dffff489ea9abab1b6bd7e320954a7d51c6ff9e64b15d2b2579ff85920ec9c9760d000000c5472341f4580680dbd8460100000000000060902c5cb870b8effbf70541f08ee438bb1e079ee71d26f3f0a2e431f97d7fbb5e2af2f955cb1876a652a90bec1a0000008a8b4682e8b10c00b7b18d02000000000000c020f37dbfc1340b29a52eb26b715055553552c6bf42b2c9f3bc23ec7aa9f87b6ed5d829f933bb06000080e2a191207a2c03c06d6ca3000000000000005004beef9f2c6997dcd4d8d838d4aec78152ea6a19ff76d3f864d74a25954a1d2befffaee406bb06000080e2a091207a2c03c06d6ca3000000000000005024f5f5f5937cdf6f95fcc8f3bc83ec7a1cc8d82f947449eaed5aa9747f8e1b9552cbe573dccfae0300006070d148103d9601e036b6510000000000000028a2cacaca614110dce5fbfecfe5e709763d0e3ccf3bde5c654a29b524aaab75d5d7d71f2063f8b1695e33bfdb750000000c1e1a09a2c73200dcc6360a00000000000000251004c17cdff777cacf4bed5a1c789e3741c6bfd6343cd5d5d58db5eba5609ad7e4fd7f207959c673885d070000c0e0a091207a2c03c06d6ca3000000000000005022beefcf926c95dc1ad595a206c2343c29a5fe3108829fc8cfa976bd54e4fd6f94cff0179ee71d63d700000030703412448f6500b88d6d14000000000000004a28088289e64a514aa9e752a9d478bb1e0732fe3f97eca8adadfdb25d2b15f91cbf2663e890cff054bb0600008081a191207a2c03c06d6ca30000000000000050629ee7eda794baddf7fd77ebeaea66d8f53890b1574a3a8320f80bbb562af2fe179affc84d0821841042063ff6772f9416cb00701bdb2800000000000000442410beefef9428bb1607a954ea6819fb5b92fbcded06ed7a295457578fb6a70100000071473307e036b651000000000000008890526aa6effbbf0c82e03673e52bbbeeba9a9a9a31320f3f94f1b72c58b0e060bb0e000000207f3473006e631b050000000000008088799e37c1f7fd35411034c7b169a9b1b171a88cfd3b320fefcacf13ec3a00000080fcd0cc01b88d6d14000000000000001c609a967cdfffae644b2a953ac9aec78152aacedc22517efe895d03000000d07f3473006e631b05000000000000008728a5bceea6a53abb1607a954ea5419fffb926bec1a00000080fea19903701bdb2800000000000000384629f587beefff4272476565e530bbee3acff38e90b16f943cd2d0d030caae03000000c88d660ec06d6ca300000000000000e0a086868671beefff38088216cff30eb1ebae9331ef2fe36f92bc347ffefcc3ed3a00000080bed1cc01b88d6d14000000000000001cd5d8d8383408829b7ddfdf964aa5be68d7e340c67eade43dc9c9760d00000040763473006e631b0500000000000000c7f9be7f8964a7526a815d8b0319f745dde3f7ed1a000000804c3473006e631b050000000000008018504afd81effb3f939ff75456560eb3ebaeababab9b21e3df62aed8250f2bec3a00000080dfa39903701bdb2800000000000000c484e7790729a57e1804c17ac9a176dd7532e68966ec320fab655e0eb4eb00000000f6a09903701bdb2800000000000000c44b85effb8d92f752a9d4a976d1750b172e1c2e637f4029f586fc3ccaae03000000a09903701ddb2800000000000000c490effb35922ec957ed5a1c0441f05732f68edadadab3ec1a00000090743473006e631b050000000000008098f23c6fbaeffb6f4bee37578eb2ebaeabadad3d57c6be43f275bb060000002419cd1c80dbd8460100000000000020c66a6a6ac628a556fbbeffc2fcf9f30fb7ebae4ba552c7cad87f2af3708fe779fbd975000000208968e600dcc6360a00000000000000f157e1fbfe62c9fb41109c6e175de779de4132f61fcbd89b53a9d478bb0e000000240dcd1c80dbd84601000000000000a04ca452a90b7cdfef0a82e07fdb35d73536360e554add2ee3ffb98cff38bb0e0000002409cd1c80dbd84601000000000000a08c789e778ceffb6f29a596cbef23ecbaeb64ec0ddd4d637f6cd700000080a4a09903701bdb28000000000000009419cff30ef47dff09c906f9fd08bbee3a19f76cc9074aa945760d00000048029a3900b7b18d020000000000004099524a7dcb342e054170a65d739d8c7b8ae455c98371bc521700000030103473006e631b050000000000008032964aa5e6fabebf43f29776cd75d5d5d5a3bbafd4f542100487da75000000a05cd1cc01b88d6d1400000000000000ca5c2a953adaf7fdd7cdd5a21a1a1a46d975c755c8b86f906c93f938c92e02000000e588660ec06d6ca3000000000000009000e66a514110fc8beffb1b2553ecbaeb64ec979affa04d08218410424852627f2706e00eb651000000000000004810dff7af9274044170b65d739d691ab3a70100000000506a345c0100000000000040c2f8be3f47d2a994bac2ae010000000080dc68b80200000000000080044aa5529ff77dff55c90acff3f6b7eb0000000000203b1aae0000000000000020a14ca395effb8f98c62bd38065d7010000000040261aae0000000000000020e18220b8d2dc62507e9e63d70000000000c0de68b8020000000000000098ff585c29e90882e06fec1a0000000000f83d1aae000000000000000021dff7a748364a9aaaff7fbbf6cf22e51586711837c6c88a120941c44a10410281402020085b095bcc62e1e179ceceee6620b069ec53a4c880450a8b14292c52d85b0829048b4040102c054104114514dcfd141e3b39266137fb4767e6bae0c7cb3bf7279897673098ef77000000c0c1150000000000ef188d468733f346eb4129e574bf030000c0ac73700500000000c07b22e24a66be6ecf8bfd06000000b3ccc1150000000000ff28332fb45eb57eea3700000098550eae0000000000f857a594539979bfd67a737575f548bf030000c0ac71700500000000c07f5a5c5cfc2c22fec8cc87a59433fd0e000000b3c4c11500000000005b526bfd3133372262b1df000000605638b8020000000060cb32f37ceb6544fcdc5e0ff43b0000004c3b075700000000006ccbcacacac9ccbcd7bab5b4b474b4df010000609a39b8020000000060db4a298732f37aeb51449ced7700000098560eae0000000000f8df32f387d646ad75d06f000000308d1c5c0100000000b02399f95deb45eb97f67aa0df010000609a38b8020000000060c76aad2732f36eebcfe17078acdf010000605a38b802000000006057acafaf7f9a99bfd75a1f47c4b97e0700008069e0e00a00000000805d556bfd3e3337dbf352bf010000c0a473700500000000c0aecbcc6f5bcf5b57c7e3f15cbf030000c0a472700500000000c09e28a57c99997f47c4edd168f479bf030000c02472700500000000c09e595858381811bf65e6935aeb57fd0e00000093c6c11500000000007b2e228699b9d9badc6f00000030491c5c0100000000b02f32f39bd6b35aebafe3f178aedf010000601238b8020000000060dfacadad7d91997f45c49de5e5e5e3fd0e0000001f3b07570000000000ecab52ca279979adf5b4d6fa75bf030000c0c7ccc11500000000001f4444e4db8fd492244992244d5afd7f5c0000000000d81783c160beff0d000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000080e9f00633a78b3ce8ce22d90000000049454e44ae426082', true);
INSERT INTO public.act_ge_bytearray VALUES ('2f76bf9b-0816-11f1-8e1e-b6c4454722a1', 1, 'source', NULL, '\x7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a323433362e302c2279223a3938352e307d2c2275707065724c656674223a7b2278223a302e302c2279223a302e307d7d2c227265736f757263654964223a2263616e766173222c227374656e63696c223a7b226964223a2242504d4e4469616772616d227d2c227374656e63696c736574223a7b226e616d657370616365223a22687474703a2f2f62336d6e2e6f72672f7374656e63696c7365742f62706d6e322e3023222c2275726c223a222e2e2f656469746f722f7374656e63696c736574732f62706d6e322e302f62706d6e322e302e6a736f6e227d2c2270726f70657274696573223a7b2270726f636573735f6964223a2275736572576f726b666c6f77222c226e616d65223a225573657220576f726b666c6f77222c2270726f636573735f6e616d657370616365223a22687474703a2f2f7777772e666c6f7761626c652e6f72672f70726f63657373646566222c2269736561676572657865637574696f6e6665746368223a66616c73652c226d65737361676573223a5b5d2c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d2c226576656e746c697374656e657273223a7b226576656e744c697374656e657273223a5b5d7d2c227369676e616c646566696e6974696f6e73223a5b5d2c226d657373616765646566696e6974696f6e73223a5b5d2c22657363616c6174696f6e646566696e6974696f6e73223a5b5d7d2c226368696c64536861706573223a5b7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3336332e302c2279223a3238322e307d2c2275707065724c656674223a7b2278223a3333332e302c2279223a3235322e307d7d2c227265736f757263654964223a227468655374617274222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253746172744e6f6e654576656e74227d2c2270726f70657274696573223a7b226f766572726964656964223a227468655374617274222c22696e74657272757074696e67223a747275652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f7731227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f7731222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a31352e302c2279223a31352e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22637265617465227d5d2c22746172676574223a7b227265736f757263654964223a22637265617465227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f7731227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3430302e302c2279223a3338332e307d2c2275707065724c656674223a7b2278223a3330302e302c2279223a3332332e307d7d2c227265736f757263654964223a22637265617465222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22536572766963655461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a22637265617465222c226e616d65223a22437265617465222c22736572766963657461736b64656c656761746565787072657373696f6e223a22247b6372656174657d222c22736572766963657461736b6669656c6473223a7b226669656c6473223a5b5d7d2c22736572766963657461736b657863657074696f6e73223a7b22657863657074696f6e73223a5b5d7d2c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f7732227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f7732222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a32302e302c2279223a32302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a226372656174654757227d5d2c22746172676574223a7b227265736f757263654964223a226372656174654757227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f7732227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3337302e302c2279223a3534372e307d2c2275707065724c656674223a7b2278223a3333302e302c2279223a3530372e307d7d2c227265736f757263654964223a226372656174654757222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a224578636c757369766547617465776179227d2c2270726f70657274696573223a7b226f766572726964656964223a226372656174654757222c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22637265617465324163746976617465227d2c7b227265736f757263654964223a226372656174654173416e6f6e796d6f757332417070726f76616c227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3532302e302c2279223a3632302e307d2c2275707065724c656674223a7b2278223a3432302e302c2279223a3536302e307d7d2c227265736f757263654964223a22637265617465417070726f76616c222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22557365725461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a22637265617465417070726f76616c222c226e616d65223a2243726561746520617070726f76616c222c22757365727461736b61737369676e6d656e74223a7b2261737369676e6d656e74223a7b2274797065223a22737461746963222c2263616e64696461746547726f757073223a5b7b2276616c7565223a226d616e6167696e674469726563746f72227d5d7d7d2c22666f726d6b6579646566696e6974696f6e223a22637265617465417070726f76616c222c22666f726d70726f70657274696573223a7b22666f726d50726f70657274696573223a5b7b226964223a22757365726e616d65222c226e616d65223a22557365726e616d65222c2274797065223a22737472696e67222c2265787072657373696f6e223a22247b75736572544f2e757365726e616d657d222c227661726961626c65223a6e756c6c2c2264656661756c74223a6e756c6c2c227265717569726564223a66616c73652c227265616461626c65223a747275652c227772697461626c65223a66616c73657d2c7b226964223a22617070726f7665437265617465222c226e616d65223a22417070726f76653f222c2274797065223a22626f6f6c65616e222c2265787072657373696f6e223a6e756c6c2c227661726961626c65223a22617070726f7665437265617465222c2264656661756c74223a6e756c6c2c227265717569726564223a747275652c227265616461626c65223a747275652c227772697461626c65223a747275657d2c7b226964223a2272656a656374526561736f6e222c226e616d65223a22526561736f6e20666f722072656a656374696e67222c2274797065223a22737472696e67222c2265787072657373696f6e223a6e756c6c2c227661726961626c65223a2272656a656374526561736f6e222c2264656661756c74223a6e756c6c2c227265717569726564223a66616c73652c227265616461626c65223a747275652c227772697461626c65223a747275657d5d7d2c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c227461736b6c697374656e657273223a7b227461736b4c697374656e657273223a5b5d7d2c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a227369642d39333044414446312d433336312d343344442d413234302d353832463231444542394236227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3537372e352c2279223a3834352e307d2c2275707065724c656674223a7b2278223a3437372e352c2279223a3736352e307d7d2c227265736f757263654964223a22437265617465417070726f76616c4576616c756174696f6e222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a225363726970745461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a22437265617465417070726f76616c4576616c756174696f6e222c226e616d65223a2243726561746520617070726f76616c206576616c756174696f6e222c22736372697074666f726d6174223a2267726f6f7679222c2273637269707474657874223a225c6e747279207b5c6e202069662028617070726f7665437265617465297b5c6e20202020657865637574696f6e2e7365745661726961626c65285c227461736b5c222c2027617070726f766527293b5c6e20207d20656c7365207b5c6e20202020657865637574696f6e2e7365745661726961626c65285c227461736b5c222c202772656a65637427293b5c6e20207d5c6e7d20636174636828457863657074696f6e20616529207b5c6e2020747279207b5c6e20202020696620287461736b20213d202764656c6574652729207b5c6e202020202020657865637574696f6e2e7365745661726961626c65285c227461736b5c222c206e756c6c293b5c6e202020207d5c6e20207d20636174636828457863657074696f6e20746529207b5c6e20202020657865637574696f6e2e7365745661726961626c65285c227461736b5c222c206e756c6c293b5c6e20207d5c6e7d2066696e616c6c79207b5c6e2020657865637574696f6e2e72656d6f76655661726961626c65285c22617070726f76654372656174655c22293b5c6e2020657865637574696f6e2e72656d6f76655661726961626c65285c2272656a656374526561736f6e5c22293b5c6e7d222c22736b697065787072657373696f6e223a6e756c6c2c227363726970746175746f73746f72657661726961626c6573223a66616c73652c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a227369642d35364537303431412d373438412d344337312d414246332d374443413042454338393931227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3631302e302c2279223a3631302e307d2c2275707065724c656674223a7b2278223a3537302e302c2279223a3537302e307d7d2c227265736f757263654964223a22637265617465417070726f76616c4757222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a224578636c757369766547617465776179227d2c2270726f70657274696573223a7b226f766572726964656964223a22637265617465417070726f76616c4757222c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a227369642d37364238324236382d303939442d343732392d423843462d443032383338364645393030227d2c7b227265736f757263654964223a227369642d42324545433531312d323932342d344139352d423042382d453335444132363844443538227d2c7b227265736f757263654964223a22637265617465417070726f76616c475732456e61626c654757227d2c7b227265736f757263654964223a22637265617465417070726f76616c3252656a656374227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3733302e302c2279223a3436392e307d2c2275707065724c656674223a7b2278223a3639302e302c2279223a3432392e307d7d2c227265736f757263654964223a22656e61626c654757222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a224578636c757369766547617465776179227d2c2270726f70657274696573223a7b226f766572726964656964223a22656e61626c654757222c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22656e61626c6547573247656e6572617465546f6b656e227d2c7b227265736f757263654964223a22656e61626c654757324163746976617465227d2c7b227265736f757263654964223a22656e61626c65475732416374697665227d2c7b227265736f757263654964223a22656e61626c6547573253757370656e646564227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22656e61626c6547573247656e6572617465546f6b656e222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a3836302e302c2279223a3434392e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2267656e6572617465546f6b656e227d5d2c22746172676574223a7b227265736f757263654964223a2267656e6572617465546f6b656e227d2c2270726f70657274696573223a7b226f766572726964656964223a22656e61626c6547573247656e6572617465546f6b656e222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b666c6f7761626c655574696c732e697355736572496e67726f75702875736572544f2c202767726f7570466f72576f726b666c6f774f7074496e27297d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22656e61626c654757324163746976617465222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a3737322e302c2279223a3434392e307d2c7b2278223a3737322e302c2279223a3634302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a226163746976617465227d5d2c22746172676574223a7b227265736f757263654964223a226163746976617465227d2c2270726f70657274696573223a7b226f766572726964656964223a22656e61626c654757324163746976617465222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b656e61626c6564203d3d206e756c6c7d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22656e61626c65475732416374697665222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a3836312e302c2279223a3434392e307d2c7b2278223a3836312e302c2279223a3534312e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22616374697665227d5d2c22746172676574223a7b227265736f757263654964223a22616374697665227d2c2270726f70657274696573223a7b226f766572726964656964223a22656e61626c65475732416374697665222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b656e61626c65647d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22656e61626c6547573253757370656e646564222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a3737322e302c2279223a3434392e307d2c7b2278223a3737322e302c2279223a3133302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2273757370656e64227d5d2c22746172676574223a7b227265736f757263654964223a2273757370656e64227d2c2270726f70657274696573223a7b226f766572726964656964223a22656e61626c6547573253757370656e646564222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b21656e61626c65647d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3931302e302c2279223a3637302e307d2c2275707065724c656674223a7b2278223a3831302e302c2279223a3631302e307d7d2c227265736f757263654964223a226163746976617465222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22536572766963655461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a226163746976617465222c226e616d65223a224163746976617465222c22736572766963657461736b64656c656761746565787072657373696f6e223a22247b6175746f41637469766174657d222c22736572766963657461736b6669656c6473223a7b226669656c6473223a5b5d7d2c22736572766963657461736b657863657074696f6e73223a7b22657863657074696f6e73223a5b5d7d2c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f7734227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f7734222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a3932322e302c2279223a3634302e307d2c7b2278223a313035392e302c2279223a3634302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22616374697665227d5d2c22746172676574223a7b227265736f757263654964223a22616374697665227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f7734227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3931302e302c2279223a3237332e307d2c2275707065724c656674223a7b2278223a3831302e302c2279223a3231332e307d7d2c227265736f757263654964223a2267656e6572617465546f6b656e222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22536572766963655461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a2267656e6572617465546f6b656e222c226e616d65223a2247656e657261746520746f6b656e222c22736572766963657461736b64656c656761746565787072657373696f6e223a22247b67656e6572617465546f6b656e7d222c22736572766963657461736b6669656c6473223a7b226669656c6473223a5b5d7d2c22736572766963657461736b657863657074696f6e73223a7b22657863657074696f6e73223a5b5d7d2c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f7735227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f7735222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2263726561746564227d5d2c22746172676574223a7b227265736f757263654964223a2263726561746564227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f7735227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313035322e302c2279223a3237332e307d2c2275707065724c656674223a7b2278223a3935322e302c2279223a3231332e307d7d2c227265736f757263654964223a2263726561746564222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22557365725461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a2263726561746564222c226e616d65223a2243726561746564222c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c227461736b6c697374656e657273223a7b227461736b4c697374656e657273223a5b5d7d2c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f7736227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f7736222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a313030322e302c2279223a3331302e307d2c7b2278223a313132322e302c2279223a3331302e307d2c7b2278223a32302e302c2279223a32302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a226f7074696e4757227d5d2c22746172676574223a7b227265736f757263654964223a226f7074696e4757227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f7736227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313134322e302c2279223a3236332e307d2c2275707065724c656674223a7b2278223a313130322e302c2279223a3232332e307d7d2c227265736f757263654964223a226f7074696e4757222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a224578636c757369766547617465776179227d2c2270726f70657274696573223a7b226f766572726964656964223a226f7074696e4757222c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a2263726561746564324163746976617465227d2c7b227265736f757263654964223a22637265617465643243726561746564227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a2263726561746564324163746976617465222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2272656d6f7665546f6b656e227d5d2c22746172676574223a7b227265736f757263654964223a2272656d6f7665546f6b656e227d2c2270726f70657274696573223a7b226f766572726964656964223a2263726561746564324163746976617465222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b757365722e636865636b546f6b656e28746f6b656e297d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22637265617465643243726561746564222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2263726561746564227d5d2c22746172676574223a7b227265736f757263654964223a2263726561746564227d2c2270726f70657274696573223a7b226f766572726964656964223a22637265617465643243726561746564222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b21757365722e636865636b546f6b656e28746f6b656e297d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313238302e302c2279223a3237332e307d2c2275707065724c656674223a7b2278223a313138302e302c2279223a3231332e307d7d2c227265736f757263654964223a2272656d6f7665546f6b656e222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a225363726970745461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a2272656d6f7665546f6b656e222c226e616d65223a2252656d6f766520546f6b656e20616e64204163746976617465222c22736372697074666f726d6174223a2267726f6f7679222c2273637269707474657874223a225c6e2020202020202020757365722e72656d6f7665546f6b656e28295c6e202020202020222c22736b697065787072657373696f6e223a6e756c6c2c227363726970746175746f73746f72657661726961626c6573223a66616c73652c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f7737227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f7737222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22616374697665227d5d2c22746172676574223a7b227265736f757263654964223a22616374697665227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f7737227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313233302e302c2279223a3537312e307d2c2275707065724c656674223a7b2278223a313133302e302c2279223a3531312e307d7d2c227265736f757263654964223a22616374697665222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22557365725461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a22616374697665222c226e616d65223a22416374697665222c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c227461736b6c697374656e657273223a7b227461736b4c697374656e657273223a5b5d7d2c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f7738227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f7738222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a32302e302c2279223a32302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a226163746976654777227d5d2c22746172676574223a7b227265736f757263654964223a226163746976654777227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f7738227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313534302e302c2279223a3536302e307d2c2275707065724c656674223a7b2278223a313530302e302c2279223a3532302e307d7d2c227265736f757263654964223a226163746976654777222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a224578636c757369766547617465776179227d2c2270726f70657274696573223a7b226f766572726964656964223a226163746976654777222c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a2261637469766532557064617465417070726f76616c227d2c7b227265736f757263654964223a226163746976653244656c657465417070726f76616c227d2c7b227265736f757263654964223a2261637469766532557064617465227d2c7b227265736f757263654964223a226163746976653253757370656e64227d2c7b227265736f757263654964223a226163746976653244656c657465227d2c7b227265736f757263654964223a22616374697665325265717565737450617373776f72645265736574227d2c7b227265736f757263654964223a2261637469766532436f6e6669726d50617373776f72645265736574227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a2261637469766532557064617465417070726f76616c222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a313535342e302c2279223a3534302e307d2c7b2278223a313535342e302c2279223a3737302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22757064617465417070726f76616c227d5d2c22746172676574223a7b227265736f757263654964223a22757064617465417070726f76616c227d2c2270726f70657274696573223a7b226f766572726964656964223a2261637469766532557064617465417070726f76616c222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b77664578656375746f72203d3d20757365722e676574557365726e616d65282920616e64207461736b203d3d202775706461746527205c6e2020202020202020616e642028217573657255522e6765744d656d626572736869707328292e6973456d7074792829297d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a226163746976653244656c657465417070726f76616c222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a313535342e302c2279223a3534302e307d2c7b2278223a313535342e302c2279223a3331302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2264656c657465417070726f76616c227d5d2c22746172676574223a7b227265736f757263654964223a2264656c657465417070726f76616c227d2c2270726f70657274696573223a7b226f766572726964656964223a226163746976653244656c657465417070726f76616c222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b77664578656375746f72203d3d20757365722e676574557365726e616d65282920616e64207461736b203d3d202764656c657465277d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a2261637469766532557064617465222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a313535362e302c2279223a3534302e307d2c7b2278223a313535362e302c2279223a3733302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22757064617465227d5d2c22746172676574223a7b227265736f757263654964223a22757064617465227d2c2270726f70657274696573223a7b226f766572726964656964223a2261637469766532557064617465222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b7461736b203d3d2027757064617465277d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a226163746976653253757370656e64222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a313535322e302c2279223a3534302e307d2c7b2278223a313535322e302c2279223a3133302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2273757370656e64227d5d2c22746172676574223a7b227265736f757263654964223a2273757370656e64227d2c2270726f70657274696573223a7b226f766572726964656964223a226163746976653253757370656e64222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b7461736b203d3d202773757370656e64277d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a226163746976653244656c657465222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a313535322e302c2279223a3534302e307d2c7b2278223a313535322e302c2279223a3638302e307d2c7b2278223a323137342e302c2279223a3638302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2264656c657465227d5d2c22746172676574223a7b227265736f757263654964223a2264656c657465227d2c2270726f70657274696573223a7b226f766572726964656964223a226163746976653244656c657465222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b7461736b203d3d202764656c657465277d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22616374697665325265717565737450617373776f72645265736574222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a313639332e302c2279223a3534302e307d2c7b2278223a35302e302c2279223a32392e357d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2267656e6572617465546f6b656e3450617373776f72645265736574227d5d2c22746172676574223a7b227265736f757263654964223a2267656e6572617465546f6b656e3450617373776f72645265736574227d2c2270726f70657274696573223a7b226f766572726964656964223a22616374697665325265717565737450617373776f72645265736574222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b7461736b203d3d20277265717565737450617373776f72645265736574277d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a2261637469766532436f6e6669726d50617373776f72645265736574222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a313639332e302c2279223a3534302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22636865636b546f6b656e34436f6e6669726d50617373776f72645265736574227d5d2c22746172676574223a7b227265736f757263654964223a22636865636b546f6b656e34436f6e6669726d50617373776f72645265736574227d2c2270726f70657274696573223a7b226f766572726964656964223a2261637469766532436f6e6669726d50617373776f72645265736574222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b7461736b203d3d2027636f6e6669726d50617373776f72645265736574277d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313730352e302c2279223a3830302e307d2c2275707065724c656674223a7b2278223a313630352e302c2279223a3734302e307d7d2c227265736f757263654964223a22757064617465417070726f76616c222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22557365725461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a22757064617465417070726f76616c222c226e616d65223a2255706461746520617070726f76616c222c22757365727461736b61737369676e6d656e74223a7b2261737369676e6d656e74223a7b2274797065223a22737461746963222c2263616e64696461746547726f757073223a5b7b2276616c7565223a226d616e6167696e674469726563746f72227d5d7d7d2c22666f726d6b6579646566696e6974696f6e223a22757064617465417070726f76616c222c22666f726d70726f70657274696573223a7b22666f726d50726f70657274696573223a5b7b226964223a22757365726e616d65222c226e616d65223a22557365726e616d65222c2274797065223a22737472696e67222c2265787072657373696f6e223a22247b75736572544f2e757365726e616d657d222c227661726961626c65223a6e756c6c2c2264656661756c74223a6e756c6c2c227265717569726564223a66616c73652c227265616461626c65223a747275652c227772697461626c65223a66616c73657d2c7b226964223a22617070726f7665557064617465222c226e616d65223a22417070726f76653f222c2274797065223a22626f6f6c65616e222c2265787072657373696f6e223a6e756c6c2c227661726961626c65223a22617070726f7665557064617465222c2264656661756c74223a6e756c6c2c227265717569726564223a747275652c227265616461626c65223a747275652c227772697461626c65223a747275657d2c7b226964223a2272656a656374526561736f6e222c226e616d65223a22526561736f6e20666f722072656a656374696e67222c2274797065223a22737472696e67222c2265787072657373696f6e223a6e756c6c2c227661726961626c65223a2272656a656374526561736f6e222c2264656661756c74223a6e756c6c2c227265717569726564223a66616c73652c227265616461626c65223a747275652c227772697461626c65223a747275657d5d7d2c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c227461736b6c697374656e657273223a7b227461736b4c697374656e657273223a5b5d7d2c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a227369642d41324244463830332d363838432d344134442d394433332d364438353943303239323435227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313834302e302c2279223a3933352e307d2c2275707065724c656674223a7b2278223a313734302e302c2279223a3835352e307d7d2c227265736f757263654964223a22557064617465417070726f76616c4576616c756174696f6e222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a225363726970745461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a22557064617465417070726f76616c4576616c756174696f6e222c226e616d65223a2255706461746520617070726f76616c206576616c756174696f6e222c22736372697074666f726d6174223a2267726f6f7679222c2273637269707474657874223a225c6e747279207b5c6e202069662028617070726f7665557064617465297b5c6e20202020657865637574696f6e2e7365745661726961626c65285c227461736b5c222c2027617070726f766527293b5c6e20207d20656c7365207b5c6e20202020657865637574696f6e2e7365745661726961626c65285c227461736b5c222c202772656a65637427293b5c6e20207d5c6e7d20636174636828457863657074696f6e20616529207b5c6e2020747279207b5c6e20202020696620287461736b20213d202764656c6574652729207b5c6e202020202020657865637574696f6e2e7365745661726961626c65285c227461736b5c222c206e756c6c293b5c6e202020207d5c6e20207d20636174636828457863657074696f6e20746529207b5c6e20202020657865637574696f6e2e7365745661726961626c65285c227461736b5c222c206e756c6c293b5c6e20207d5c6e7d2066696e616c6c79207b5c6e2020657865637574696f6e2e72656d6f76655661726961626c65285c22617070726f76655570646174655c22293b5c6e2020657865637574696f6e2e72656d6f76655661726961626c65285c2272656a656374526561736f6e5c22293b5c6e7d222c22736b697065787072657373696f6e223a6e756c6c2c227363726970746175746f73746f72657661726961626c6573223a66616c73652c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a227369642d35334641374632392d434536302d344145362d393231442d414637333331434442313946227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313831302e302c2279223a3739302e307d2c2275707065724c656674223a7b2278223a313737302e302c2279223a3735302e307d7d2c227265736f757263654964223a22757064617465417070726f76616c4757222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a224578636c757369766547617465776179227d2c2270726f70657274696573223a7b226f766572726964656964223a22757064617465417070726f76616c4757222c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a227369642d45353842424332442d383833312d344346322d413739382d313442323538464535363942227d2c7b227265736f757263654964223a22757064617465417070726f76616c475732557064617465227d2c7b227265736f757263654964223a22757064617465417070726f76616c47573252656a656374227d2c7b227265736f757263654964223a227369642d42354646454243412d314642462d343537462d424335352d333946443338373138384232227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a227369642d45353842424332442d383833312d344346322d413739382d313442323538464535363942222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22757064617465417070726f76616c227d5d2c22746172676574223a7b227265736f757263654964223a22757064617465417070726f76616c227d2c2270726f70657274696573223a7b226f766572726964656964223a227369642d45353842424332442d383833312d344346322d413739382d313442323538464535363942222c2264656661756c74666c6f77223a747275657d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22757064617465417070726f76616c475732557064617465222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a313835322e302c2279223a3737302e307d2c7b2278223a313835322e302c2279223a3733302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22757064617465227d5d2c22746172676574223a7b227265736f757263654964223a22757064617465227d2c2270726f70657274696573223a7b226f766572726964656964223a22757064617465417070726f76616c475732557064617465222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b7461736b203d3d2027617070726f7665277d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22757064617465417070726f76616c47573252656a656374222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a313835322e302c2279223a3737302e307d2c7b2278223a313835322e302c2279223a3837302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2272656a656374557064617465227d5d2c22746172676574223a7b227265736f757263654964223a2272656a656374557064617465227d2c2270726f70657274696573223a7b226f766572726964656964223a22757064617465417070726f76616c47573252656a656374222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b7461736b203d3d202772656a656374277d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a227369642d42354646454243412d314642462d343537462d424335352d333946443338373138384232222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a313739302e302c2279223a3832312e307d2c7b2278223a323237302e302c2279223a3832312e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2264656c657465227d5d2c22746172676574223a7b227265736f757263654964223a2264656c657465227d2c2270726f70657274696573223a7b226f766572726964656964223a227369642d42354646454243412d314642462d343537462d424335352d333946443338373138384232222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b7461736b203d3d202764656c657465277d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313939302e302c2279223a3930302e307d2c2275707065724c656674223a7b2278223a313839302e302c2279223a3834302e307d7d2c227265736f757263654964223a2272656a656374557064617465222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a225363726970745461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a2272656a656374557064617465222c226e616d65223a2252656a65637420757064617465222c22736372697074666f726d6174223a2267726f6f7679222c2273637269707474657874223a225c6e2020202020202020657865637574696f6e2e7365745661726961626c65285c2270726f7042795265736f757263655c222c206e756c6c293b5c6e202020202020222c22736b697065787072657373696f6e223a6e756c6c2c227363726970746175746f73746f72657661726961626c6573223a66616c73652c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f7738746572227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f7738746572222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a313934302e302c2279223a3934382e307d2c7b2278223a313333392e302c2279223a3934382e307d2c7b2278223a313231342e302c2279223a3833372e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22616374697665227d5d2c22746172676574223a7b227265736f757263654964223a22616374697665227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f7738746572227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313939302e302c2279223a3736302e307d2c2275707065724c656674223a7b2278223a313839302e302c2279223a3730302e307d7d2c227265736f757263654964223a22757064617465222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22536572766963655461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a22757064617465222c226e616d65223a22557064617465222c22736572766963657461736b64656c656761746565787072657373696f6e223a22247b7570646174657d222c22736572766963657461736b6669656c6473223a7b226669656c6473223a5b5d7d2c22736572766963657461736b657863657074696f6e73223a7b22657863657074696f6e73223a5b5d7d2c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f7739227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f7739222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a323033372e302c2279223a3733302e307d2c7b2278223a323033372e302c2279223a3937312e307d2c7b2278223a313333342e302c2279223a3937312e307d2c7b2278223a313138302e302c2279223a3834342e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22616374697665227d5d2c22746172676574223a7b227265736f757263654964223a22616374697665227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f7739227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313639302e302c2279223a3136302e307d2c2275707065724c656674223a7b2278223a313539302e302c2279223a3130302e307d7d2c227265736f757263654964223a2273757370656e64222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22536572766963655461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a2273757370656e64222c226e616d65223a2253757370656e64222c22736572766963657461736b64656c656761746565787072657373696f6e223a22247b73757370656e647d222c22736572766963657461736b6669656c6473223a7b226669656c6473223a5b5d7d2c22736572766963657461736b657863657074696f6e73223a7b22657863657074696f6e73223a5b5d7d2c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f773130227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f773130222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a313730322e302c2279223a3133302e307d2c7b2278223a313730322e302c2279223a3135302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2273757370656e646564227d5d2c22746172676574223a7b227265736f757263654964223a2273757370656e646564227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f773130227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313834302e302c2279223a3138302e307d2c2275707065724c656674223a7b2278223a313734302e302c2279223a3132302e307d7d2c227265736f757263654964223a2273757370656e646564222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22557365725461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a2273757370656e646564222c226e616d65223a2253757370656e646564222c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c227461736b6c697374656e657273223a7b227461736b4c697374656e657273223a5b5d7d2c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f773131227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f773131222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a313837392e302c2279223a3136352e307d2c7b2278223a313837392e302c2279223a3230302e307d2c7b2278223a32302e302c2279223a32302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2273757370656e6465644777227d5d2c22746172676574223a7b227265736f757263654964223a2273757370656e6465644777227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f773131227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313936302e302c2279223a3232302e307d2c2275707065724c656674223a7b2278223a313932302e302c2279223a3138302e307d7d2c227265736f757263654964223a2273757370656e6465644777222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a224578636c757369766547617465776179227d2c2270726f70657274696573223a7b226f766572726964656964223a2273757370656e6465644777222c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a2273757370656e6465643252656163746976617465227d2c7b227265736f757263654964223a2273757370656e6465643244656c657465227d2c7b227265736f757263654964223a227369642d32364143444543342d384245322d344537302d424236332d333642363337354237454543227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a2273757370656e6465643252656163746976617465222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a323030322e302c2279223a3230302e307d2c7b2278223a323030322e302c2279223a3134302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2272656163746976617465227d5d2c22746172676574223a7b227265736f757263654964223a2272656163746976617465227d2c2270726f70657274696573223a7b226f766572726964656964223a2273757370656e6465643252656163746976617465222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b7461736b203d3d202772656163746976617465277d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a2273757370656e6465643244656c657465222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a323237302e302c2279223a3230302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2264656c657465227d5d2c22746172676574223a7b227265736f757263654964223a2264656c657465227d2c2270726f70657274696573223a7b226f766572726964656964223a2273757370656e6465643244656c657465222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b7461736b203d3d202764656c657465277d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a323134302e302c2279223a3137302e307d2c2275707065724c656674223a7b2278223a323034302e302c2279223a3131302e307d7d2c227265736f757263654964223a2272656163746976617465222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22536572766963655461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a2272656163746976617465222c226e616d65223a2252656163746976617465222c22736572766963657461736b64656c656761746565787072657373696f6e223a22247b726561637469766174657d222c22736572766963657461736b6669656c6473223a7b226669656c6473223a5b5d7d2c22736572766963657461736b657863657074696f6e73223a7b22657863657074696f6e73223a5b5d7d2c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f773132227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f773132222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a323032382e302c2279223a3132372e307d2c7b2278223a313437382e302c2279223a34302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22616374697665227d5d2c22746172676574223a7b227265736f757263654964223a22616374697665227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f773132227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3736302e302c2279223a3731392e307d2c2275707065724c656674223a7b2278223a3636302e302c2279223a3635392e307d7d2c227265736f757263654964223a2272656a656374222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a225363726970745461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a2272656a656374222c226e616d65223a2252656a656374222c22736372697074666f726d6174223a2267726f6f7679222c2273637269707474657874223a225c6e2020202020202020657865637574696f6e2e72656d6f76655661726961626c65285c2275736572544f5c22293b5c6e2020202020202020657865637574696f6e2e72656d6f76655661726961626c65285c22656e637279707465645077645c22293b5c6e2020202020202020657865637574696f6e2e72656d6f76655661726961626c65285c2270726f7042795265736f757263655c22293b5c6e202020202020222c22736b697065787072657373696f6e223a6e756c6c2c227363726970746175746f73746f72657661726961626c6573223a66616c73652c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f773133227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3931302e302c2279223a3833302e307d2c2275707065724c656674223a7b2278223a3831302e302c2279223a3737302e307d7d2c227265736f757263654964223a2272656a6563746564222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22557365725461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a2272656a6563746564222c226e616d65223a2252656a6563746564222c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c227461736b6c697374656e657273223a7b227461736b4c697374656e657273223a5b5d7d2c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f773134227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f773134222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a3836302e302c2279223a3733322e307d2c7b2278223a313031302e302c2279223a3733322e307d2c7b2278223a32302e302c2279223a32302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2272656a65637465644777227d5d2c22746172676574223a7b227265736f757263654964223a2272656a65637465644777227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f773134227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313033302e302c2279223a3832302e307d2c2275707065724c656674223a7b2278223a3939302e302c2279223a3738302e307d7d2c227265736f757263654964223a2272656a65637465644777222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a224578636c757369766547617465776179227d2c2270726f70657274696573223a7b226f766572726964656964223a2272656a65637465644777222c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a2272656a65637465643244656c657465227d2c7b227265736f757263654964223a2272656a65637465643252656a6563746564227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a2272656a65637465643244656c657465222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a313037322e302c2279223a3830302e307d2c7b2278223a313332332e302c2279223a313030302e307d2c7b2278223a323237302e302c2279223a313030302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2264656c657465227d5d2c22746172676574223a7b227265736f757263654964223a2264656c657465227d2c2270726f70657274696573223a7b226f766572726964656964223a2272656a65637465643244656c657465222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b7461736b203d3d202764656c657465277d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a2272656a65637465643252656a6563746564222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2272656a6563746564227d5d2c22746172676574223a7b227265736f757263654964223a2272656a6563746564227d2c2270726f70657274696573223a7b226f766572726964656964223a2272656a65637465643252656a6563746564222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b656d707479207461736b7d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313639302e302c2279223a3334302e307d2c2275707065724c656674223a7b2278223a313539302e302c2279223a3238302e307d7d2c227265736f757263654964223a2264656c657465417070726f76616c222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22557365725461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a2264656c657465417070726f76616c222c226e616d65223a2244656c65746520617070726f76616c222c22757365727461736b61737369676e6d656e74223a7b2261737369676e6d656e74223a7b2274797065223a22737461746963222c2263616e64696461746547726f757073223a5b7b2276616c7565223a226d616e6167696e674469726563746f72227d5d7d7d2c22666f726d6b6579646566696e6974696f6e223a2264656c657465417070726f76616c222c22666f726d70726f70657274696573223a7b22666f726d50726f70657274696573223a5b7b226964223a22757365726e616d65222c226e616d65223a22557365726e616d65222c2274797065223a22737472696e67222c2265787072657373696f6e223a22247b75736572544f2e757365726e616d657d222c227661726961626c65223a6e756c6c2c2264656661756c74223a6e756c6c2c227265717569726564223a66616c73652c227265616461626c65223a747275652c227772697461626c65223a66616c73657d2c7b226964223a22617070726f766544656c657465222c226e616d65223a22417070726f76653f222c2274797065223a22626f6f6c65616e222c2265787072657373696f6e223a6e756c6c2c227661726961626c65223a22617070726f766544656c657465222c2264656661756c74223a6e756c6c2c227265717569726564223a747275652c227265616461626c65223a747275652c227772697461626c65223a747275657d2c7b226964223a2272656a656374526561736f6e222c226e616d65223a22526561736f6e20666f722072656a656374696e67222c2274797065223a22737472696e67222c2265787072657373696f6e223a6e756c6c2c227661726961626c65223a2272656a656374526561736f6e222c2264656661756c74223a6e756c6c2c227265717569726564223a66616c73652c227265616461626c65223a747275652c227772697461626c65223a747275657d5d7d2c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c227461736b6c697374656e657273223a7b227461736b4c697374656e657273223a5b5d7d2c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f773134626973227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f773134626973222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a313730322e302c2279223a3331302e307d2c7b2278223a313730322e302c2279223a3332302e307d2c7b2278223a32302e302c2279223a32302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2264656c657465417070726f76616c4757227d5d2c22746172676574223a7b227265736f757263654964223a2264656c657465417070726f76616c4757227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f773134626973227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313831302e302c2279223a3334302e307d2c2275707065724c656674223a7b2278223a313737302e302c2279223a3330302e307d7d2c227265736f757263654964223a2264656c657465417070726f76616c4757222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a224578636c757369766547617465776179227d2c2270726f70657274696573223a7b226f766572726964656964223a2264656c657465417070726f76616c4757222c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a2264656c657465417070726f76616c47573244656c657465227d2c7b227265736f757263654964223a2264656c657465417070726f76616c47573252656a656374227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a2264656c657465417070726f76616c47573244656c657465222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a313835322e302c2279223a3332302e307d2c7b2278223a313835322e302c2279223a3338382e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2264656c657465227d5d2c22746172676574223a7b227265736f757263654964223a2264656c657465227d2c2270726f70657274696573223a7b226f766572726964656964223a2264656c657465417070726f76616c47573244656c657465222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b617070726f76657d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a2264656c657465417070726f76616c47573252656a656374222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a313835322e302c2279223a3332302e307d2c7b2278223a313835322e302c2279223a3237302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2272656a65637444656c657465227d5d2c22746172676574223a7b227265736f757263654964223a2272656a65637444656c657465227d2c2270726f70657274696573223a7b226f766572726964656964223a2264656c657465417070726f76616c47573252656a656374222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b21617070726f76657d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313939302e302c2279223a3330302e307d2c2275707065724c656674223a7b2278223a313839302e302c2279223a3234302e307d7d2c227265736f757263654964223a2272656a65637444656c657465222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a225363726970745461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a2272656a65637444656c657465222c226e616d65223a2252656a6563742064656c657465222c22736372697074666f726d6174223a2267726f6f7679222c2273637269707474657874223a225c6e2020202020202020657865637574696f6e2e7365745661726961626c65285c2270726f7042795265736f757263655c222c206e756c6c293b5c6e202020202020222c22736b697065787072657373696f6e223a6e756c6c2c227363726970746175746f73746f72657661726961626c6573223a66616c73652c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f773134746572227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f773134746572222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a313532342e302c2279223a3139332e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22616374697665227d5d2c22746172676574223a7b227265736f757263654964223a22616374697665227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f773134746572227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313734332e302c2279223a3531312e307d2c2275707065724c656674223a7b2278223a313634332e302c2279223a3435322e307d7d2c227265736f757263654964223a2267656e6572617465546f6b656e3450617373776f72645265736574222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22536572766963655461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a2267656e6572617465546f6b656e3450617373776f72645265736574222c226e616d65223a2247656e657261746520746f6b656e222c22736572766963657461736b64656c656761746565787072657373696f6e223a22247b67656e6572617465546f6b656e7d222c22736572766963657461736b6669656c6473223a7b226669656c6473223a5b5d7d2c22736572766963657461736b657863657074696f6e73223a7b22657863657074696f6e73223a5b5d7d2c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f773135227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f773135222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a32392e357d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a226e6f74696679345265717565737450617373776f72645265736574227d5d2c22746172676574223a7b227265736f757263654964223a226e6f74696679345265717565737450617373776f72645265736574227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f773135227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313934382e302c2279223a3531322e307d2c2275707065724c656674223a7b2278223a313834382e302c2279223a3435322e307d7d2c227265736f757263654964223a226e6f74696679345265717565737450617373776f72645265736574222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22536572766963655461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a226e6f74696679345265717565737450617373776f72645265736574222c226e616d65223a224e6f74696669636174696f6e222c22736572766963657461736b64656c656761746565787072657373696f6e223a22247b6e6f746966797d222c22736572766963657461736b6669656c6473223a7b226669656c6473223a5b5d7d2c22736572766963657461736b657863657074696f6e73223a7b22657863657074696f6e73223a5b5d7d2c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f773136227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f773136222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a313639322e302c2279223a3338372e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22616374697665227d5d2c22746172676574223a7b227265736f757263654964223a22616374697665227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f773136227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313734332e302c2279223a3632312e307d2c2275707065724c656674223a7b2278223a313634332e302c2279223a3536312e307d7d2c227265736f757263654964223a22636865636b546f6b656e34436f6e6669726d50617373776f72645265736574222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22536572766963655461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a22636865636b546f6b656e34436f6e6669726d50617373776f72645265736574222c226e616d65223a22436865636b20746f6b656e2c2072656d6f766520616e64207570646174652070617373776f7264222c22736572766963657461736b64656c656761746565787072657373696f6e223a22247b70617373776f726452657365747d222c22736572766963657461736b6669656c6473223a7b226669656c6473223a5b5d7d2c22736572766963657461736b657863657074696f6e73223a7b22657863657074696f6e73223a5b5d7d2c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f773137227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f773137222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a226e6f7469667934436f6e6669726d50617373776f72645265736574227d5d2c22746172676574223a7b227265736f757263654964223a226e6f7469667934436f6e6669726d50617373776f72645265736574227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f773137227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313934382e302c2279223a3632312e307d2c2275707065724c656674223a7b2278223a313834382e302c2279223a3536312e307d7d2c227265736f757263654964223a226e6f7469667934436f6e6669726d50617373776f72645265736574222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22536572766963655461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a226e6f7469667934436f6e6669726d50617373776f72645265736574222c226e616d65223a224e6f74696669636174696f6e222c22736572766963657461736b64656c656761746565787072657373696f6e223a22247b6e6f746966797d222c22736572766963657461736b6669656c6473223a7b226669656c6473223a5b5d7d2c22736572766963657461736b657863657074696f6e73223a7b22657863657074696f6e73223a5b5d7d2c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f773138227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f773138222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a313737332e302c2279223a3636322e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22616374697665227d5d2c22746172676574223a7b227265736f757263654964223a22616374697665227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f773138227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a323332302e302c2279223a3431382e307d2c2275707065724c656674223a7b2278223a323232302e302c2279223a3335382e307d7d2c227265736f757263654964223a2264656c657465222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22536572766963655461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a2264656c657465222c226e616d65223a2244656c657465222c22736572766963657461736b64656c656761746565787072657373696f6e223a22247b64656c6574657d222c22736572766963657461736b6669656c6473223a7b226669656c6473223a5b5d7d2c22736572766963657461736b657863657074696f6e73223a7b22657863657074696f6e73223a5b5d7d2c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f773939227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f773939222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a31342e302c2279223a31342e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22746865456e64227d5d2c22746172676574223a7b227265736f757263654964223a22746865456e64227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f773939227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a323338362e302c2279223a3430322e307d2c2275707065724c656674223a7b2278223a323335382e302c2279223a3337342e307d7d2c227265736f757263654964223a22746865456e64222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22456e644e6f6e654576656e74227d2c2270726f70657274696573223a7b226f766572726964656964223a22746865456e64222c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3436322e352c2279223a3833302e307d2c2275707065724c656674223a7b2278223a3336322e352c2279223a3737302e307d7d2c227265736f757263654964223a227570646174655768696c6550656e64696e67437265617465417070726f76616c222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22536572766963655461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a227570646174655768696c6550656e64696e67437265617465417070726f76616c222c226e616d65223a22557064617465222c22736572766963657461736b64656c656761746565787072657373696f6e223a22247b7570646174657d222c22736572766963657461736b6669656c6473223a7b226669656c6473223a5b5d7d2c22736572766963657461736b657863657074696f6e73223a7b22657863657074696f6e73223a5b5d7d2c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a227369642d46373436453738462d463539422d343834322d394438382d433943373934373446433036227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a227369642d37364238324236382d303939442d343732392d423843462d443032383338364645393030222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22637265617465417070726f76616c227d5d2c22746172676574223a7b227265736f757263654964223a22637265617465417070726f76616c227d2c2270726f70657274696573223a7b226f766572726964656964223a227369642d37364238324236382d303939442d343732392d423843462d443032383338364645393030222c2264656661756c74666c6f77223a747275657d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a227369642d42324545433531312d323932342d344139352d423042382d453335444132363844443538222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a3539302e302c2279223a3835392e307d2c7b2278223a313031302e302c2279223a3835392e307d2c7b2278223a32302e302c2279223a32302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2272656a65637465644777227d5d2c22746172676574223a7b227265736f757263654964223a2272656a65637465644777227d2c2270726f70657274696573223a7b226f766572726964656964223a227369642d42324545433531312d323932342d344139352d423042382d453335444132363844443538222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b7461736b203d3d202764656c657465277d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3439302e302c2279223a3730302e307d2c2275707065724c656674223a7b2278223a3435302e302c2279223a3636302e307d7d2c227265736f757263654964223a227369642d38434641333135322d313941412d343837382d414432432d393642363346364539384337222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a224578636c757369766547617465776179227d2c2270726f70657274696573223a7b226f766572726964656964223a227369642d38434641333135322d313941412d343837382d414432432d393642363346364539384337222c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a22666c6f7733227d2c7b227265736f757263654964223a227369642d43394641353941462d344633392d343839452d423432342d354330364231354145333137227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f773133222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a3737322e302c2279223a3638392e307d2c7b2278223a3737322e302c2279223a3830302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2272656a6563746564227d5d2c22746172676574223a7b227265736f757263654964223a2272656a6563746564227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f773133227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22637265617465417070726f76616c475732456e61626c654757222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a3632322e302c2279223a3539302e307d2c7b2278223a3632322e302c2279223a3434392e307d2c7b2278223a32302e302c2279223a32302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22656e61626c654757227d5d2c22746172676574223a7b227265736f757263654964223a22656e61626c654757227d2c2270726f70657274696573223a7b226f766572726964656964223a22637265617465417070726f76616c475732456e61626c654757222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b7461736b203d3d2027617070726f7665277d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22637265617465417070726f76616c3252656a656374222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a3632322e302c2279223a3539302e307d2c7b2278223a3632322e302c2279223a3638392e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2272656a656374227d5d2c22746172676574223a7b227265736f757263654964223a2272656a656374227d2c2270726f70657274696573223a7b226f766572726964656964223a22637265617465417070726f76616c3252656a656374222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b7461736b203d3d202772656a656374277d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22637265617465324163746976617465222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a3338322e302c2279223a3532372e307d2c7b2278223a3338322e302c2279223a3434392e307d2c7b2278223a32302e302c2279223a32302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22656e61626c654757227d5d2c22746172676574223a7b227265736f757263654964223a22656e61626c654757227d2c2270726f70657274696573223a7b226f766572726964656964223a22637265617465324163746976617465222c2264656661756c74666c6f77223a747275657d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a22666c6f7733222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a3532372e302c2279223a3638302e307d2c7b2278223a35302e302c2279223a34302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22437265617465417070726f76616c4576616c756174696f6e227d5d2c22746172676574223a7b227265736f757263654964223a22437265617465417070726f76616c4576616c756174696f6e227d2c2270726f70657274696573223a7b226f766572726964656964223a22666c6f7733222c2264656661756c74666c6f77223a747275657d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a227369642d35364537303431412d373438412d344337312d414246332d374443413042454338393931222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a34302e307d2c7b2278223a3539302e302c2279223a3830352e307d2c7b2278223a32302e302c2279223a32302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22637265617465417070726f76616c4757227d5d2c22746172676574223a7b227265736f757263654964223a22637265617465417070726f76616c4757227d2c2270726f70657274696573223a7b226f766572726964656964223a227369642d35364537303431412d373438412d344337312d414246332d374443413042454338393931227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a226372656174654173416e6f6e796d6f757332417070726f76616c222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a3338322e302c2279223a3532372e307d2c7b2278223a3338322e302c2279223a3539302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22637265617465417070726f76616c227d5d2c22746172676574223a7b227265736f757263654964223a22637265617465417070726f76616c227d2c2270726f70657274696573223a7b226f766572726964656964223a226372656174654173416e6f6e796d6f757332417070726f76616c222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b77664578656375746f72203d3d2027616e6f6e796d6f757327207c7c20666c6f7761626c655574696c732e697355736572496e67726f75702875736572544f2c202767726f7570466f72576f726b666c6f77417070726f76616c27297d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313535352e302c2279223a3932352e307d2c2275707065724c656674223a7b2278223a313435352e302c2279223a3836352e307d7d2c227265736f757263654964223a227570646174655768696c6550656e64696e67557064617465417070726f76616c222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a22536572766963655461736b227d2c2270726f70657274696573223a7b226f766572726964656964223a227570646174655768696c6550656e64696e67557064617465417070726f76616c222c226e616d65223a22557064617465222c22736572766963657461736b64656c656761746565787072657373696f6e223a22247b7570646174657d222c22736572766963657461736b6669656c6473223a7b226669656c6473223a5b5d7d2c22736572766963657461736b657863657074696f6e73223a7b22657863657074696f6e73223a5b5d7d2c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c226973666f72636f6d70656e736174696f6e223a66616c73652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a227369642d31443044413539332d383544372d343132422d384441332d373944433043453030453144227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a313637352e302c2279223a3931352e307d2c2275707065724c656674223a7b2278223a313633352e302c2279223a3837352e307d7d2c227265736f757263654964223a227369642d34433943393131372d323644422d343332362d413132422d454544413432454144463037222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a224578636c757369766547617465776179227d2c2270726f70657274696573223a7b226f766572726964656964223a227369642d34433943393131372d323644422d343332362d413132422d454544413432454144463037222c226173796e6368726f6e6f7573646566696e6974696f6e223a66616c73652c226578636c7573697665646566696e6974696f6e223a747275652c22657865637574696f6e6c697374656e657273223a7b22657865637574696f6e4c697374656e657273223a5b5d7d7d2c226f7574676f696e67223a5b7b227265736f757263654964223a227369642d36364344423143342d374332462d344241452d393833342d383443463043463532424633227d2c7b227265736f757263654964223a227369642d31324532394239342d433336392d343543312d424345462d433136354146444135323541227d5d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a227369642d39333044414446312d433336312d343344442d413234302d353832463231444542394236222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a32302e302c2279223a32302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a227369642d38434641333135322d313941412d343837382d414432432d393642363346364539384337227d5d2c22746172676574223a7b227265736f757263654964223a227369642d38434641333135322d313941412d343837382d414432432d393642363346364539384337227d2c2270726f70657274696573223a7b226f766572726964656964223a227369642d39333044414446312d433336312d343344442d413234302d353832463231444542394236227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a227369642d41324244463830332d363838432d344134442d394433332d364438353943303239323435222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a32302e302c2279223a32302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a227369642d34433943393131372d323644422d343332362d413132422d454544413432454144463037227d5d2c22746172676574223a7b227265736f757263654964223a227369642d34433943393131372d323644422d343332362d413132422d454544413432454144463037227d2c2270726f70657274696573223a7b226f766572726964656964223a227369642d41324244463830332d363838432d344134442d394433332d364438353943303239323435227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a227369642d36364344423143342d374332462d344241452d393833342d383443463043463532424633222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a227570646174655768696c6550656e64696e67557064617465417070726f76616c227d5d2c22746172676574223a7b227265736f757263654964223a227570646174655768696c6550656e64696e67557064617465417070726f76616c227d2c2270726f70657274696573223a7b226f766572726964656964223a227369642d36364344423143342d374332462d344241452d393833342d383443463043463532424633222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b7461736b203d3d2027757064617465277d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a227369642d43394641353941462d344633392d343839452d423432342d354330364231354145333137222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a3431322e302c2279223a3638302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a227570646174655768696c6550656e64696e67437265617465417070726f76616c227d5d2c22746172676574223a7b227265736f757263654964223a227570646174655768696c6550656e64696e67437265617465417070726f76616c227d2c2270726f70657274696573223a7b226f766572726964656964223a227369642d43394641353941462d344633392d343839452d423432342d354330364231354145333137222c22636f6e646974696f6e73657175656e6365666c6f77223a22247b7461736b203d3d2027757064617465277d227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a227369642d46373436453738462d463539422d343834322d394438382d433943373934373446433036222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a3336372e302c2279223a3638332e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22637265617465417070726f76616c227d5d2c22746172676574223a7b227265736f757263654964223a22637265617465417070726f76616c227d2c2270726f70657274696573223a7b226f766572726964656964223a227369642d46373436453738462d463539422d343834322d394438382d433943373934373446433036227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a227369642d31443044413539332d383544372d343132422d384441332d373944433043453030453144222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a33302e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22757064617465417070726f76616c227d5d2c22746172676574223a7b227265736f757263654964223a22757064617465417070726f76616c227d2c2270726f70657274696573223a7b226f766572726964656964223a227369642d31443044413539332d383544372d343132422d384441332d373944433043453030453144227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a227369642d31324532394239342d433336392d343543312d424345462d433136354146444135323541222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a35302e302c2279223a34302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22557064617465417070726f76616c4576616c756174696f6e227d5d2c22746172676574223a7b227265736f757263654964223a22557064617465417070726f76616c4576616c756174696f6e227d2c2270726f70657274696573223a7b226f766572726964656964223a227369642d31324532394239342d433336392d343543312d424345462d433136354146444135323541222c2264656661756c74666c6f77223a747275657d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a227369642d35334641374632392d434536302d344145362d393231442d414637333331434442313946222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a35302e302c2279223a34302e307d2c7b2278223a32302e302c2279223a32302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a22757064617465417070726f76616c4757227d5d2c22746172676574223a7b227265736f757263654964223a22757064617465417070726f76616c4757227d2c2270726f70657274696573223a7b226f766572726964656964223a227369642d35334641374632392d434536302d344145362d393231442d414637333331434442313946227d7d2c7b22626f756e6473223a7b226c6f7765725269676874223a7b2278223a3137322e302c2279223a3231322e307d2c2275707065724c656674223a7b2278223a3132382e302c2279223a3231322e307d7d2c227265736f757263654964223a227369642d32364143444543342d384245322d344537302d424236332d333642363337354237454543222c226368696c64536861706573223a5b5d2c227374656e63696c223a7b226964223a2253657175656e6365466c6f77227d2c22646f636b657273223a5b7b2278223a32302e302c2279223a32302e307d2c7b2278223a313934302e302c2279223a3133352e307d2c7b2278223a35302e302c2279223a33302e307d5d2c226f7574676f696e67223a5b7b227265736f757263654964223a2273757370656e646564227d5d2c22746172676574223a7b227265736f757263654964223a2273757370656e646564227d2c2270726f70657274696573223a7b226f766572726964656964223a227369642d32364143444543342d384245322d344537302d424236332d333642363337354237454543222c2264656661756c74666c6f77223a747275657d7d5d7d', NULL);


--
-- TOC entry 4880 (class 0 OID 18001)
-- Dependencies: 326
-- Data for Name: act_ge_property; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.act_ge_property VALUES ('common.schema.version', '7.2.0.2', 1);
INSERT INTO public.act_ge_property VALUES ('next.dbid', '1', 1);
INSERT INTO public.act_ge_property VALUES ('schema.version', '7.2.0.2', 1);
INSERT INTO public.act_ge_property VALUES ('schema.history', 'create(7.2.0.2)', 1);
INSERT INTO public.act_ge_property VALUES ('cfg.execution-related-entities-count', 'true', 1);
INSERT INTO public.act_ge_property VALUES ('cfg.task-related-entities-count', 'true', 1);


--
-- TOC entry 4910 (class 0 OID 18550)
-- Dependencies: 356
-- Data for Name: act_hi_actinst; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4913 (class 0 OID 18573)
-- Dependencies: 359
-- Data for Name: act_hi_attachment; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4912 (class 0 OID 18566)
-- Dependencies: 358
-- Data for Name: act_hi_comment; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4911 (class 0 OID 18559)
-- Dependencies: 357
-- Data for Name: act_hi_detail; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4883 (class 0 OID 18024)
-- Dependencies: 329
-- Data for Name: act_hi_entitylink; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4885 (class 0 OID 18047)
-- Dependencies: 331
-- Data for Name: act_hi_identitylink; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4909 (class 0 OID 18539)
-- Dependencies: 355
-- Data for Name: act_hi_procinst; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4895 (class 0 OID 18221)
-- Dependencies: 341
-- Data for Name: act_hi_taskinst; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4897 (class 0 OID 18231)
-- Dependencies: 343
-- Data for Name: act_hi_tsk_log; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4899 (class 0 OID 18259)
-- Dependencies: 345
-- Data for Name: act_hi_varinst; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4915 (class 0 OID 18603)
-- Dependencies: 361
-- Data for Name: act_id_bytearray; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4916 (class 0 OID 18610)
-- Dependencies: 362
-- Data for Name: act_id_group; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4919 (class 0 OID 18630)
-- Dependencies: 365
-- Data for Name: act_id_info; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4917 (class 0 OID 18617)
-- Dependencies: 363
-- Data for Name: act_id_membership; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4921 (class 0 OID 18644)
-- Dependencies: 367
-- Data for Name: act_id_priv; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4922 (class 0 OID 18649)
-- Dependencies: 368
-- Data for Name: act_id_priv_mapping; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4914 (class 0 OID 18598)
-- Dependencies: 360
-- Data for Name: act_id_property; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.act_id_property VALUES ('schema.version', '7.2.0.2', 1);


--
-- TOC entry 4920 (class 0 OID 18637)
-- Dependencies: 366
-- Data for Name: act_id_token; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4918 (class 0 OID 18622)
-- Dependencies: 364
-- Data for Name: act_id_user; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4907 (class 0 OID 18325)
-- Dependencies: 353
-- Data for Name: act_procdef_info; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4901 (class 0 OID 18282)
-- Dependencies: 347
-- Data for Name: act_re_deployment; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.act_re_deployment VALUES ('2f285176-0816-11f1-8e1e-b6c4454722a1', NULL, NULL, NULL, '', '2026-02-12 14:24:39.867', NULL, NULL, '2f285176-0816-11f1-8e1e-b6c4454722a1', NULL);


--
-- TOC entry 4902 (class 0 OID 18290)
-- Dependencies: 348
-- Data for Name: act_re_model; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.act_re_model VALUES ('2f7008da-0816-11f1-8e1e-b6c4454722a1', 2, 'User Workflow', NULL, NULL, '2026-02-12 14:24:40.337', '2026-02-12 14:24:40.381', 1, '{"name":"User Workflow"}', '2f285176-0816-11f1-8e1e-b6c4454722a1', '2f76bf9b-0816-11f1-8e1e-b6c4454722a1', NULL, '');


--
-- TOC entry 4904 (class 0 OID 18306)
-- Dependencies: 350
-- Data for Name: act_re_procdef; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.act_re_procdef VALUES ('userWorkflow:1:2f6a6389-0816-11f1-8e1e-b6c4454722a1', 1, 'http://www.flowable.org/processdef', 'User Workflow', 'userWorkflow', 1, '2f285176-0816-11f1-8e1e-b6c4454722a1', 'userWorkflow.bpmn20.xml', 'userWorkflow.userWorkflow.png', NULL, false, true, 1, '', NULL, NULL, 0, NULL);


--
-- TOC entry 4908 (class 0 OID 18330)
-- Dependencies: 354
-- Data for Name: act_ru_actinst; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4889 (class 0 OID 18082)
-- Dependencies: 335
-- Data for Name: act_ru_deadletter_job; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4882 (class 0 OID 18013)
-- Dependencies: 328
-- Data for Name: act_ru_entitylink; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4900 (class 0 OID 18270)
-- Dependencies: 346
-- Data for Name: act_ru_event_subscr; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4903 (class 0 OID 18298)
-- Dependencies: 349
-- Data for Name: act_ru_execution; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.act_ru_execution VALUES ('1', 2, '1', 'userWorkflow:74cd8ece-715a-44a4-a736-e17b46c4e7e6', NULL, 'userWorkflow:1:2f6a6389-0816-11f1-8e1e-b6c4454722a1', NULL, NULL, 'active', true, false, true, false, NULL, 0, NULL, '', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.act_ru_execution VALUES ('3', 2, '3', 'userWorkflow:b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee', NULL, 'userWorkflow:1:2f6a6389-0816-11f1-8e1e-b6c4454722a1', NULL, NULL, 'active', true, false, true, false, NULL, 0, NULL, '', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.act_ru_execution VALUES ('5', 2, '5', 'userWorkflow:c9b2dec2-00a7-4855-97c0-d854842b4b24', NULL, 'userWorkflow:1:2f6a6389-0816-11f1-8e1e-b6c4454722a1', NULL, NULL, 'active', true, false, true, false, NULL, 0, NULL, '', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.act_ru_execution VALUES ('7', 2, '7', 'userWorkflow:823074dc-d280-436d-a7dd-07399fae48ec', NULL, 'userWorkflow:1:2f6a6389-0816-11f1-8e1e-b6c4454722a1', NULL, NULL, 'active', true, false, true, false, NULL, 0, NULL, '', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.act_ru_execution VALUES ('9', 2, '9', 'userWorkflow:1417acbe-cbf6-4277-9372-e75e04f97000', NULL, 'userWorkflow:1:2f6a6389-0816-11f1-8e1e-b6c4454722a1', NULL, NULL, 'active', true, false, true, false, NULL, 0, NULL, '', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);


--
-- TOC entry 4891 (class 0 OID 18098)
-- Dependencies: 337
-- Data for Name: act_ru_external_job; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4890 (class 0 OID 18090)
-- Dependencies: 336
-- Data for Name: act_ru_history_job; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4884 (class 0 OID 18035)
-- Dependencies: 330
-- Data for Name: act_ru_identitylink; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4886 (class 0 OID 18058)
-- Dependencies: 332
-- Data for Name: act_ru_job; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4888 (class 0 OID 18074)
-- Dependencies: 334
-- Data for Name: act_ru_suspended_job; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4894 (class 0 OID 18209)
-- Dependencies: 340
-- Data for Name: act_ru_task; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.act_ru_task VALUES ('2', 2, '1', '1', 'userWorkflow:1:2f6a6389-0816-11f1-8e1e-b6c4454722a1', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Active', NULL, NULL, 'active', NULL, NULL, NULL, 50, '2010-10-20 00:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.act_ru_task VALUES ('4', 2, '3', '3', 'userWorkflow:1:2f6a6389-0816-11f1-8e1e-b6c4454722a1', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Active', NULL, NULL, 'active', NULL, NULL, NULL, 50, '2010-10-20 00:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.act_ru_task VALUES ('6', 2, '5', '5', 'userWorkflow:1:2f6a6389-0816-11f1-8e1e-b6c4454722a1', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Active', NULL, NULL, 'active', NULL, NULL, NULL, 50, '2010-10-20 00:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.act_ru_task VALUES ('8', 2, '7', '7', 'userWorkflow:1:2f6a6389-0816-11f1-8e1e-b6c4454722a1', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Active', NULL, NULL, 'active', NULL, NULL, NULL, 50, '2010-10-20 00:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.act_ru_task VALUES ('10', 2, '9', '9', 'userWorkflow:1:2f6a6389-0816-11f1-8e1e-b6c4454722a1', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Active', NULL, NULL, 'active', NULL, NULL, NULL, 50, '2010-10-20 00:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, NULL, NULL, NULL, NULL);


--
-- TOC entry 4887 (class 0 OID 18066)
-- Dependencies: 333
-- Data for Name: act_ru_timer_job; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4898 (class 0 OID 18244)
-- Dependencies: 344
-- Data for Name: act_ru_variable; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4923 (class 0 OID 18681)
-- Dependencies: 369
-- Data for Name: adyngroupmembers; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4775 (class 0 OID 16416)
-- Dependencies: 221
-- Data for Name: adyngroupmembership; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4776 (class 0 OID 16423)
-- Dependencies: 222
-- Data for Name: amembership; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4777 (class 0 OID 16428)
-- Dependencies: 223
-- Data for Name: anyabout; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.anyabout VALUES ('a328f2e6-25e9-4cc1-badf-7425d7be4b39', 'token!=$null', 'USER', 'e00945b5-1184-4d43-8e45-4318a8dcdfd4');
INSERT INTO public.anyabout VALUES ('2e2ee845-2abf-43c6-b543-49243a84e2f1', 'fullname==*o*;fullname==*i*', 'USER', '9e2b911c-25de-4c77-bcea-b86ed9451050');


--
-- TOC entry 4778 (class 0 OID 16437)
-- Dependencies: 224
-- Data for Name: anyobject; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.anyobject VALUES ('fc6dbc3a-6c07-4965-8781-921e7401a4a5', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'HP LJ 1300n', '[{"schema": "model", "values": [{"stringValue": "Canon MFC8030"}]}, {"schema": "location", "values": [{"stringValue": "1st floor"}]}]', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', 'PRINTER');
INSERT INTO public.anyobject VALUES ('8559d14d-58c2-46eb-a2d4-a7d35161e8f8', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'Canon MF 8030cn', '[{"schema": "model", "values": [{"stringValue": "HP Laserjet 1300n"}]}, {"schema": "location", "values": [{"stringValue": "2nd floor"}]}]', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', 'PRINTER');
INSERT INTO public.anyobject VALUES ('9e1d130c-d6a3-48b1-98b3-182477ed0688', NULL, '2021-04-15 11:45:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'Epson Stylus Color', '[]', '0679e069-7355-4b20-bd11-a5a0a5453c7c', 'PRINTER');


--
-- TOC entry 4779 (class 0 OID 16446)
-- Dependencies: 225
-- Data for Name: anyobject_anytypeclass; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4780 (class 0 OID 16451)
-- Dependencies: 226
-- Data for Name: anyobject_externalresource; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4781 (class 0 OID 16456)
-- Dependencies: 227
-- Data for Name: anytemplatelivesynctask; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4782 (class 0 OID 16465)
-- Dependencies: 228
-- Data for Name: anytemplatepulltask; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.anytemplatepulltask VALUES ('3a6173a9-8c34-4e37-b3b1-0c2ea385fac0', '{"_class":"org.apache.syncope.common.lib.to.UserTO","creator":null,"creationDate":null,"lastModifier":null,"lastChangeDate":null,"key":null,"type":"USER","realm":null,"status":null,"password":null,"token":null,"tokenExpireTime":null,"username":null,"lastLoginDate":null,"changePwdDate":null,"failedLogins":null,"securityQuestion":null,"securityAnswer":null,"auxClasses":["csv"],"derAttrs":[{"schema":"cn","values":[""]}],"resources":["resource-testdb"],"relationships":[],"memberships":[{"groupKey":"f779c0d4-633b-4be5-8f57-32eb478a3ca5","groupName":null}],"dynMemberships":[],"roles":[],"dynRoles":[],"plainAttrs":[{"schema":"ctype","values":["email == ''test8@syncope.apache.org''? ''TYPE_8'': ''TYPE_OTHER''"]}]}', 'USER', 'c41b9b71-9bfa-4f90-89f2-84787def4c5c');
INSERT INTO public.anytemplatepulltask VALUES ('b3772d66-ec06-4133-bf38-b3273845ac5b', '{"_class":"org.apache.syncope.common.lib.to.GroupTO","creator":null,"creationDate":null,"lastModifier":null,"lastChangeDate":null,"key":null,"type":"GROUP","realm":null,"status":null,"name":null,"userOwner":null,"groupOwner":null,"udynMembershipCond":null,"auxClasses":[],"derAttrs":[],"resources":[],"plainAttrs":[]}', 'GROUP', 'c41b9b71-9bfa-4f90-89f2-84787def4c5c');
INSERT INTO public.anytemplatepulltask VALUES ('6c3f578d-327b-4a7c-8037-6f5ba24eb770', '{"_class":"org.apache.syncope.common.lib.to.UserTO","creator":null,"creationDate":null,"lastModifier":null,"lastChangeDate":null,"key":null,"type":"USER","realm":null,"status":null,"password":null,"token":null,"tokenExpireTime":null,"username":null,"lastLoginDate":null,"changePwdDate":null,"failedLogins":null,"securityQuestion":null,"securityAnswer":null,"auxClasses":[],"derAttrs":[],"resources":[],"relationships":[],"memberships":[],"dynMemberships":[],"roles":[],"dynRoles":[],"plainAttrs":[{"schema":"ctype","values":["''type a''"]},{"schema":"userId","values":["''reconciled@syncope.apache.org''"]},{"schema":"fullname","values":["''reconciled fullname''"]},{"schema":"surname","values":["''surname''"]}]}', 'USER', '83f7e85d-9774-43fe-adba-ccd856312994');
INSERT INTO public.anytemplatepulltask VALUES ('45b61137-c7c3-49ee-86e0-9efffa75ae68', '{"_class":"org.apache.syncope.common.lib.to.GroupTO","creator":null,"creationDate":null,"lastModifier":null,"lastChangeDate":null,"key":null,"type":"GROUP","realm":null,"status":null,"name":null,"userOwner":null,"groupOwner":null,"udynMembershipCond":null,"auxClasses":[],"derAttrs":[],"resources":[],"plainAttrs":[]}', 'GROUP', '83f7e85d-9774-43fe-adba-ccd856312994');
INSERT INTO public.anytemplatepulltask VALUES ('df655a2a-40c0-43b1-a157-3f4988802f58', '{"_class":"org.apache.syncope.common.lib.to.UserTO","creator":null,"creationDate":null,"lastModifier":null,"lastChangeDate":null,"key":null,"type":"USER","realm":"''/'' + title","status":null,"password":null,"token":null,"tokenExpireTime":null,"username":null,"lastLoginDate":null,"changePwdDate":null,"failedLogins":null,"securityQuestion":null,"securityAnswer":null,"auxClasses":["minimal group"],"derAttrs":[],"resources":["resource-ldap"],"roles":[],"dynRoles":[],"relationships":[],"memberships":[],"dynMemberships":[],"plainAttrs":[]}', 'USER', '1e419ca4-ea81-4493-a14f-28b90113686d');
INSERT INTO public.anytemplatepulltask VALUES ('fda22ff3-98f3-42e4-a2ae-cd9a28282d57', '{"_class":"org.apache.syncope.common.lib.to.GroupTO","creator":null,"creationDate":null,"lastModifier":null,"lastChangeDate":null,"key":null,"type":"GROUP","realm":null,"status":null,"name":null,"userOwner":null,"groupOwner":null,"udynMembershipCond":null,"auxClasses":[],"derAttrs":[],"resources":[],"plainAttrs":[{"schema":"show","values":["true"]}]}', 'GROUP', '1e419ca4-ea81-4493-a14f-28b90113686d');
INSERT INTO public.anytemplatepulltask VALUES ('8bc41ba1-cc1d-4ee0-bb43-61cd148b414f', '{"_class":"org.apache.syncope.common.lib.to.UserTO","creator":null,"creationDate":null,"lastModifier":null,"lastChangeDate":null,"key":null,"type":"USER","realm":null,"status":null,"password":null,"token":null,"tokenExpireTime":null,"username":null,"lastLoginDate":null,"changePwdDate":null,"failedLogins":null,"securityQuestion":null,"securityAnswer":null,"auxClasses":[],"derAttrs":[],"resources":["resource-testdb"],"roles":[],"dynRoles":[],"relationships":[],"memberships":[],"dynMemberships":[],"plainAttrs":[{"schema":"firstname","values":[""]},{"schema":"userId","values":["''test''"]},{"schema":"fullname","values":["''test''"]},{"schema":"surname","values":["''test''"]}]}', 'USER', '986867e2-993b-430e-8feb-aa9abb4c1dcd');
INSERT INTO public.anytemplatepulltask VALUES ('9af0e343-8a37-42d2-9bc7-6e2e3b103219', '{"_class":"org.apache.syncope.common.lib.to.GroupTO","creator":null,"creationDate":null,"lastModifier":null,"lastChangeDate":null,"key":null,"type":"GROUP","realm":null,"status":null,"name":null,"userOwner":null,"groupOwner":null,"udynMembershipCond":null,"auxClasses":[],"derAttrs":[],"resources":[],"plainAttrs":[]}', 'GROUP', '986867e2-993b-430e-8feb-aa9abb4c1dcd');


--
-- TOC entry 4783 (class 0 OID 16474)
-- Dependencies: 229
-- Data for Name: anytemplaterealm; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4784 (class 0 OID 16483)
-- Dependencies: 230
-- Data for Name: anytype; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.anytype VALUES ('USER', 'USER');
INSERT INTO public.anytype VALUES ('GROUP', 'GROUP');
INSERT INTO public.anytype VALUES ('PRINTER', 'ANY_OBJECT');


--
-- TOC entry 4786 (class 0 OID 16493)
-- Dependencies: 232
-- Data for Name: anytype_anytypeclass; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.anytype_anytypeclass VALUES ('USER', 'minimal user');
INSERT INTO public.anytype_anytypeclass VALUES ('USER', 'other');
INSERT INTO public.anytype_anytypeclass VALUES ('GROUP', 'minimal group');
INSERT INTO public.anytype_anytypeclass VALUES ('PRINTER', 'minimal printer');


--
-- TOC entry 4785 (class 0 OID 16488)
-- Dependencies: 231
-- Data for Name: anytypeclass; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.anytypeclass VALUES ('generic membership');
INSERT INTO public.anytypeclass VALUES ('minimal user');
INSERT INTO public.anytypeclass VALUES ('other');
INSERT INTO public.anytypeclass VALUES ('minimal group');
INSERT INTO public.anytypeclass VALUES ('minimal printer');
INSERT INTO public.anytypeclass VALUES ('csv');


--
-- TOC entry 4787 (class 0 OID 16498)
-- Dependencies: 233
-- Data for Name: arelationship; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.arelationship VALUES ('11a0ec66-b59b-428a-af3d-f856950ff1c5', 'fc6dbc3a-6c07-4965-8781-921e7401a4a5', '8559d14d-58c2-46eb-a2d4-a7d35161e8f8', 'inclusion');


--
-- TOC entry 4788 (class 0 OID 16505)
-- Dependencies: 234
-- Data for Name: attrreleasepolicy; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.attrreleasepolicy VALUES ('219935c7-deb3-40b3-8a9a-683037e523a2', 'DenyAttrReleasePolicy', 0, '{"_class":"org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf","releaseAttrs":{},"allowedAttrs":[],"excludedAttrs":[],"includeOnlyAttrs":[],"principalIdAttr":null,"principalAttrRepoConf":{"mergingStrategy":"MULTIVALUED","ignoreResolvedAttributes":false,"expiration":0,"timeUnit":"HOURS","attrRepos":[]}}', NULL);
INSERT INTO public.attrreleasepolicy VALUES ('319935c7-deb3-40b3-8a9a-683037e523a2', 'AllowedAttrReleasePolicy', 0, '{"_class":"org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf","releaseAttrs":{},"allowedAttrs":["cn","givenName","uid"],"excludedAttrs":[],"includeOnlyAttrs":[],"principalIdAttr":null,"principalAttrRepoConf":{"mergingStrategy":"MULTIVALUED","ignoreResolvedAttributes":false,"expiration":0,"timeUnit":"HOURS","attrRepos":[]}}', NULL);


--
-- TOC entry 4789 (class 0 OID 16512)
-- Dependencies: 235
-- Data for Name: attrrepo; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.attrrepo VALUES ('DefaultLDAPAttrRepo', NULL, 'ACTIVE', 'LDAP attr repo', NULL, '{"_class":"org.apache.syncope.common.lib.attr.LDAPAttrRepoConf","searchFilter":"cn={user}","subtreeSearch":true,"ldapUrl":"ldap://localhost:1389","bindDn":"uid=admin,ou=system","bindCredential":"secret","baseDn":"ou=People,o=isp","useAllQueryAttributes":true,"queryAttributes":{}}');
INSERT INTO public.attrrepo VALUES ('DefaultJDBCAttrRepo', NULL, 'ACTIVE', 'JDBC attr repo', NULL, '{"_class":"org.apache.syncope.common.lib.attr.JDBCAttrRepoConf","sql":"SELECT * FROM table WHERE name=?","dialect":"org.hibernate.dialect.H2Dialect","driverClass":"org.h2.Driver","url":"jdbc:h2:mem:syncopedb;DB_CLOSE_DELAY=-1","user":"username","password":"password","singleRow":true,"requireAllAttributes":true,"caseCanonicalization":"NONE","queryType":"AND","columnMappings":{},"username":[],"caseInsensitiveQueryAttributes":[],"queryAttributes":{}}');
INSERT INTO public.attrrepo VALUES ('DefaultStubAttrRepo', NULL, 'ACTIVE', 'Stub attr repo', '[{"intAttrName":"attr1","extAttrName":"identifier","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"NONE","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}]', '{"_class":"org.apache.syncope.common.lib.attr.StubAttrRepoConf","attributes":{"attr1":"value1"}}');
INSERT INTO public.attrrepo VALUES ('DefaultSyncopeAttrRepo', NULL, 'ACTIVE', 'Syncope attr repo', NULL, '{"_class":"org.apache.syncope.common.lib.attr.SyncopeAttrRepoConf","domain":"Master","searchFilter":"username=={user}","basicAuthUsername":"admin","basicAuthPassword":"password","headers":{}}');


--
-- TOC entry 4790 (class 0 OID 16519)
-- Dependencies: 236
-- Data for Name: auditconf; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.auditconf VALUES ('[LOGIC]:[SyncopeLogic]:[]:[isSelfRegAllowed]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[WA]:[AUTHENTICATION]:[]:[validate]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[WA]:[]:[AuthenticationEvent]:[auth]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[ConnectorLogic]:[]:[create]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[ConnectorLogic]:[]:[update]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[ResourceLogic]:[]:[create]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[ResourceLogic]:[]:[update]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[UserLogic]:[]:[assign]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[UserLogic]:[]:[confirmPasswordReset]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[UserLogic]:[]:[create]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[UserLogic]:[]:[deprovision]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[UserLogic]:[]:[link]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[UserLogic]:[]:[mustChangePassword]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[UserLogic]:[]:[provision]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[UserLogic]:[]:[requestPasswordReset]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[UserLogic]:[]:[selfCreate]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[UserLogic]:[]:[selfStatus]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[UserLogic]:[]:[selfUpdate]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[UserLogic]:[]:[status]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[UserLogic]:[]:[unassign]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[UserLogic]:[]:[unlink]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[UserLogic]:[]:[update]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[GroupLogic]:[]:[assign]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[GroupLogic]:[]:[create]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[GroupLogic]:[]:[deprovision]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[GroupLogic]:[]:[provisionMembers]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[GroupLogic]:[]:[unassign]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[GroupLogic]:[]:[unlink]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[GroupLogic]:[]:[update]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[AnyObjectLogic]:[]:[assign]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[AnyObjectLogic]:[]:[create]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[AnyObjectLogic]:[]:[delete]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[AnyObjectLogic]:[]:[deprovision]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[AnyObjectLogic]:[]:[link]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[AnyObjectLogic]:[]:[provision]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[AnyObjectLogic]:[]:[unassign]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[AnyObjectLogic]:[]:[unlink]:[SUCCESS]', 1);
INSERT INTO public.auditconf VALUES ('[LOGIC]:[AnyObjectLogic]:[]:[update]:[SUCCESS]', 1);


--
-- TOC entry 4791 (class 0 OID 16524)
-- Dependencies: 237
-- Data for Name: auditevent; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4792 (class 0 OID 16531)
-- Dependencies: 238
-- Data for Name: authmodule; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.authmodule VALUES ('DefaultLDAPAuthModule', NULL, 'ACTIVE', 'LDAP auth module', '[{"intAttrName":"mail","extAttrName":"email","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"NONE","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"givenName","extAttrName":"given_name","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"NONE","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"sn","extAttrName":"family_name","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"NONE","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"cn","extAttrName":"name","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"NONE","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}]', '{"_class":"org.apache.syncope.common.lib.auth.LDAPAuthModuleConf","principalAttributeId":"cn","bindDn": "uid=admin,ou=system", "bindCredential":"secret","ldapUrl":"ldap://localhost:1389","searchFilter":"cn={user}","baseDn":"ou=People,o=isp","subtreeSearch":true}');
INSERT INTO public.authmodule VALUES ('DefaultJDBCAuthModule', NULL, 'ACTIVE', 'JDBC auth module', NULL, '{"_class":"org.apache.syncope.common.lib.auth.JDBCAuthModuleConf","sql":"SELECT * FROM users_table WHERE name=?", "fieldPassword": "password"}');
INSERT INTO public.authmodule VALUES ('DefaultGoogleMfaAuthModule', NULL, 'ACTIVE', 'Google Mfa auth module', NULL, '{"_class":"org.apache.syncope.common.lib.auth.GoogleMfaAuthModuleConf","codeDigits":6,"issuer":"SyncopeTest", "label":"SyncopeTest", "timeStepSize":30, "windowSize":3}');
INSERT INTO public.authmodule VALUES ('DefaultSimpleMfaAuthModule', NULL, 'ACTIVE', 'Simple Mfa auth module', NULL, '{"_class":"org.apache.syncope.common.lib.auth.SimpleMfaAuthModuleConf","tokenLength":6, "timeToKillInSeconds":30}');
INSERT INTO public.authmodule VALUES ('DefaultDuoMfaAuthModule', NULL, 'ACTIVE', 'Duo Mfa auth module', NULL, '{"_class":"org.apache.syncope.common.lib.auth.DuoMfaAuthModuleConf","integrationKey":"DIOXVRZD2UMZ8XXMNFQ5","secretKey":"Q2IU2i8BFNd6VYflZT8Evl6lF7oPlj3PM15BmRU7", "apiHost":"theapi.duosecurity.com"}');
INSERT INTO public.authmodule VALUES ('DefaultOIDCAuthModule', NULL, 'ACTIVE', 'OIDC auth module', NULL, '{"_class":"org.apache.syncope.common.lib.auth.OIDCAuthModuleConf","discoveryUri":"https://localhost:9443/syncope-wa/oidc/.well-known/openid-configuration", "clientId":"client-id", "clientSecret": "client-secret" }');
INSERT INTO public.authmodule VALUES ('DefaultSAML2IdPAuthModule', 0, 'ACTIVE', 'SAML2 IdP auth module', NULL, '{"_class":"org.apache.syncope.common.lib.auth.SAML2IdPAuthModuleConf","keystorePassword":"p@$$word","privateKeyPassword":"p@$$word","identityProviderMetadataPath":"https://localhost:9443/syncope-wa/idp/metadata", "serviceProviderEntityId":"syncope:apache:org"}');
INSERT INTO public.authmodule VALUES ('DefaultJaasAuthModule', NULL, 'ACTIVE', 'Jaas auth module', NULL, '{"_class":"org.apache.syncope.common.lib.auth.JaasAuthModuleConf","realm":"SYNCOPE","kerberosRealmSystemProperty":"sample-value", "loginConfigType": "JavaLoginConfig", "loginConfigurationFile": "file:/etc/jaas/login.conf"}');
INSERT INTO public.authmodule VALUES ('DefaultStaticAuthModule', NULL, 'ACTIVE', 'Static auth module', NULL, '{"_class":"org.apache.syncope.common.lib.auth.StaticAuthModuleConf","users":{"syncope1": "$cynop3"}}');
INSERT INTO public.authmodule VALUES ('DefaultSyncopeAuthModule', NULL, 'ACTIVE', 'Syncope auth module', '[{"intAttrName":"syncopeUserAttr_surname","extAttrName":"family_name","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"NONE","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"syncopeUserAttr_fullname","extAttrName":"name","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"NONE","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"syncopeUserAttr_firstname","extAttrName":"given_name","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"NONE","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"syncopeUserAttr_email","extAttrName":"email","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"NONE","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"memberships","extAttrName":"groups","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"NONE","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}]', '{"_class":"org.apache.syncope.common.lib.auth.SyncopeAuthModuleConf","domain":"Master"}');
INSERT INTO public.authmodule VALUES ('DefaultOAuth20AuthModule', 0, 'ACTIVE', 'OAuth20 auth module', NULL, '{"_class":"org.apache.syncope.common.lib.auth.OAuth20AuthModuleConf","clientName":"oauth20","clientId":"OAUTH20","clientSecret":"secret","enabled":true,"customParams":{},"tokenUrl":"https://localhost/oauth2/token","responseType":"code","scope":"oauth test","userIdAttribute":"username","authUrl":"https://localhost/oauth2/auth","profileUrl":"https://localhost/oauth2/profile","withState":false,"profileVerb":"POST"}');


--
-- TOC entry 4793 (class 0 OID 16538)
-- Dependencies: 239
-- Data for Name: authpolicy; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.authpolicy VALUES ('659b9906-4b6e-4bc0-aca0-6809dff346d4', 'MyDefaultAuthPolicyConf', '{"_class":"org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf","authModules":["LdapAuthenticationTest"]}');
INSERT INTO public.authpolicy VALUES ('b912a0d4-a890-416f-9ab8-84ab077eb028', 'DefaultAuthPolicy', '{"_class":"org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf","authModules":["LdapAuthenticationTest"]}');


--
-- TOC entry 4794 (class 0 OID 16545)
-- Dependencies: 240
-- Data for Name: authprofile; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4795 (class 0 OID 16554)
-- Dependencies: 241
-- Data for Name: casspclientapp; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4796 (class 0 OID 16567)
-- Dependencies: 242
-- Data for Name: confparam; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.confparam VALUES ('password.cipher.algorithm', '"SHA1"');
INSERT INTO public.confparam VALUES ('notificationjob.cronExpression', '"0/20 * * * * ?"');
INSERT INTO public.confparam VALUES ('notification.maxRetries', '3');
INSERT INTO public.confparam VALUES ('token.length', '256');
INSERT INTO public.confparam VALUES ('token.expireTime', '60');
INSERT INTO public.confparam VALUES ('selfRegistration.allowed', 'true');
INSERT INTO public.confparam VALUES ('passwordReset.allowed', 'true');
INSERT INTO public.confparam VALUES ('passwordReset.securityQuestion', 'true');
INSERT INTO public.confparam VALUES ('authentication.attributes', '["username","userId"]');
INSERT INTO public.confparam VALUES ('authentication.statuses', '["created","active"]');
INSERT INTO public.confparam VALUES ('log.lastlogindate', 'true');
INSERT INTO public.confparam VALUES ('return.password.value', 'false');
INSERT INTO public.confparam VALUES ('jwt.lifetime.minutes', '120');
INSERT INTO public.confparam VALUES ('connector.conf.history.size', '10');
INSERT INTO public.confparam VALUES ('resource.conf.history.size', '10');


--
-- TOC entry 4797 (class 0 OID 16574)
-- Dependencies: 243
-- Data for Name: conninstance; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.conninstance VALUES ('413bf072-678a-41d3-9d20-8c453b3a39d1', 'net.tirasa.connid.bundles.missing', NULL, NULL, 'net.tirasa.connid.bundles.missing.MissingConnector', 'Errored', '[]', 'file:/home/ilgrosso/work/syncope/fork/fit/core-reference/target/bundles/', NULL, 'none', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.conninstance VALUES ('88a7a819-dab5-46b4-9b90-0b9769eabdb8', 'net.tirasa.connid.bundles.soap', '["CREATE","UPDATE","DELETE","SEARCH"]', NULL, 'net.tirasa.connid.bundles.soap.WebServiceConnector', 'ConnInstance100', '[{"schema":{"name":"endpoint","displayName":null,"helpMessage":null,"type":"java.lang.String","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["http://localhost:9080/syncope-fit-build-tools/cxf/soap/provisioning"]},{"schema":{"name":"servicename","displayName":null,"helpMessage":null,"type":"java.lang.String","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["net.tirasa.connid.bundles.soap.provisioning.interfaces.Provisioning"]}]', 'file:/home/ilgrosso/work/syncope/fork/fit/core-reference/target/bundles/', NULL, '1.5.0', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.conninstance VALUES ('5aa5b8be-7521-481a-9651-c557aea078c1', 'net.tirasa.connid.bundles.db', '["AUTHENTICATE","CREATE","UPDATE","DELETE","SEARCH","SYNC"]', NULL, 'net.tirasa.connid.bundles.db.table.DatabaseTableConnector', 'H2', '[{"schema":{"name":"disabledStatusValue","displayName":null,"helpMessage":null,"type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["false"]},{"schema":{"name":"user","displayName":null,"helpMessage":null,"type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["sa"]},{"schema":{"name":"keyColumn","displayName":null,"helpMessage":null,"type":"java.lang.String","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["id"]},{"schema":{"name":"retrievePassword","displayName":null,"helpMessage":null,"type":"boolean","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["true"]},{"schema":{"name":"cipherAlgorithm","displayName":null,"helpMessage":null,"type":"java.lang.String","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["SHA1"]},{"schema":{"name":"enabledStatusValue","displayName":null,"helpMessage":null,"type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["true"]},{"schema":{"name":"passwordColumn","displayName":null,"helpMessage":null,"type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["password"]},{"schema":{"name":"jdbcDriver","displayName":null,"helpMessage":null,"type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["org.h2.Driver"]},{"schema":{"name":"defaultStatusValue","displayName":null,"helpMessage":null,"type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["true"]},{"schema":{"name":"table","displayName":null,"helpMessage":null,"type":"java.lang.String","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["test"]},{"schema":{"name":"password","displayName":null,"helpMessage":null,"type":"org.identityconnectors.common.security.GuardedString","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["sa"]},{"schema":{"name":"statusColumn","displayName":null,"helpMessage":null,"type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["status"]},{"schema":{"name":"jdbcUrlTemplate","displayName":null,"helpMessage":null,"type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["jdbc:h2:tcp://localhost:9092/mem:testdb;DB_CLOSE_DELAY=-1"]}]', 'connid://testconnectorserver@localhost:4554', NULL, '2.4.1', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.conninstance VALUES ('5ffbb4ac-a8c3-4b44-b699-11b398a1ba08', 'net.tirasa.connid.bundles.soap', '["CREATE","UPDATE","DELETE","SEARCH"]', 10, 'net.tirasa.connid.bundles.soap.WebServiceConnector', 'ConnInstance102', '[{"schema":{"name":"servicename","displayName":null,"helpMessage":null,"type":"java.lang.String","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["net.tirasa.connid.bundles.soap.provisioning.interfaces.Provisioning"]},{"schema":{"name":"endpoint","displayName":null,"helpMessage":null,"type":"java.lang.String","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":true,"values":["http://localhost:9080/syncope-fit-build-tools/cxf/soap/provisioning"]}]', 'file:/home/ilgrosso/work/syncope/fork/fit/core-reference/target/bundles/', NULL, '1.5.0', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.conninstance VALUES ('fcf9f2b0-f7d6-42c9-84a6-61b28255a42b', 'net.tirasa.connid.bundles.soap', NULL, NULL, 'net.tirasa.connid.bundles.soap.WebServiceConnector', 'ConnInstance103', '[{"schema":{"name":"endpoint","displayName":null,"helpMessage":null,"type":"java.lang.String","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["http://localhost:9080/syncope-fit-build-tools/cxf/soap/provisioning"]},{"schema":{"name":"servicename","displayName":null,"helpMessage":null,"type":"java.lang.String","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["net.tirasa.connid.bundles.soap.provisioning.interfaces.Provisioning"]}]', 'file:/home/ilgrosso/work/syncope/fork/fit/core-reference/target/bundles/', NULL, '1.5.0', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.conninstance VALUES ('6c2acf1b-b052-46f0-8c56-7a8ad6905edf', 'net.tirasa.connid.bundles.csvdir', '["CREATE","UPDATE","DELETE","SEARCH","SYNC"]', NULL, 'net.tirasa.connid.bundles.csvdir.CSVDirConnector', 'CSVDir', '[{"schema":{"name":"fields","displayName":"fields","helpMessage":"Column names separated by comma","type":"[Ljava.lang.String;","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["id","name","surname","email","password","theirgroup","membership","status","deleted"]},{"schema":{"name":"keyColumnNames","displayName":"Key column name","helpMessage":"Name of the column used to identify user uniquely","type":"[Ljava.lang.String;","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["name","surname"]},{"schema":{"name":"deleteColumnName","displayName":"Delete column name","helpMessage":"Name of the column used to specify users to be deleted","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["deleted"]},{"schema":{"name":"passwordColumnName","displayName":"Password column name","helpMessage":"Name of the column used to specify user password","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["password"]},{"schema":{"name":"keyseparator","displayName":"Key separator","helpMessage":"Character used to separate keys in a multi-key scenario","type":"java.lang.String","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":[","]},{"schema":{"name":"ignoreHeader","displayName":"Ignore header","helpMessage":"Specify it first line file must be ignored","type":"java.lang.Boolean","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":[false]},{"schema":{"name":"fieldDelimiter","displayName":"fieldDelimiter","helpMessage":"fieldDelimiter","type":"char","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":[","]},{"schema":{"name":"quotationRequired","displayName":"Value quotation required","helpMessage":"Specify if value quotation is required","type":"java.lang.Boolean","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":[false]},{"schema":{"name":"statusColumn","displayName":"statusColumn","helpMessage":"Status column","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["status"]},{"schema":{"name":"sourcePath","displayName":"Source path","helpMessage":"Absolute path of a directory where are located CSV files to be processed","type":"java.lang.String","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["/home/ilgrosso/work/syncope/fork/fit/core-reference/target/test-csvdir"]},{"schema":{"name":"fileMask","displayName":"File mask","helpMessage":"Regular expression describing files to be processed","type":"java.lang.String","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["test.csv"]}]', 'file:/home/ilgrosso/work/syncope/fork/fit/core-reference/target/bundles/', NULL, '0.8.9', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.conninstance VALUES ('74141a3b-0762-4720-a4aa-fc3e374ef3ef', 'net.tirasa.connid.bundles.ldap', '["CREATE","UPDATE","UPDATE_DELTA","DELETE","SEARCH"]', NULL, 'net.tirasa.connid.bundles.ldap.LdapConnector', 'TestLDAP', '[{"schema":{"name":"host","type":"java.lang.String","required":true,"order":1,"confidential":false,"defaultValues":[]},"values":["localhost"],"overridable":false},{"schema":{"name":"port","type":"int","required":false,"order":2,"confidential":false,"defaultValues":[389]},"values":[1389],"overridable":false},{"schema":{"name":"ssl","type":"boolean","required":false,"order":3,"confidential":false,"defaultValues":[false]},"values":["false"],"overridable":false},{"schema":{"name":"failover","type":"[Ljava.lang.String;","required":false,"order":4,"confidential":false,"defaultValues":[]},"values":[],"overridable":false},{"schema":{"name":"principal","type":"java.lang.String","required":false,"order":5,"confidential":false,"defaultValues":[]},"values":["uid=admin,ou=system"],"overridable":false},{"schema":{"name":"credentials","type":"org.identityconnectors.common.security.GuardedString","required":false,"order":6,"confidential":true,"defaultValues":[]},"values":["secret"],"overridable":false},{"schema":{"name":"baseContexts","type":"[Ljava.lang.String;","required":true,"order":7,"confidential":false,"defaultValues":[]},"values":["ou=people,o=isp","ou=groups,o=isp"],"overridable":true},{"schema":{"name":"passwordAttribute","type":"java.lang.String","required":false,"order":8,"confidential":false,"defaultValues":["userPassword"]},"values":["userpassword"],"overridable":false},{"schema":{"name":"accountObjectClasses","type":"[Ljava.lang.String;","required":false,"order":9,"confidential":false,"defaultValues":["top","person","organizationalPerson","inetOrgPerson"]},"values":["inetOrgPerson"],"overridable":false},{"schema":{"name":"accountUserNameAttributes","type":"[Ljava.lang.String;","required":false,"order":10,"confidential":false,"defaultValues":["uid","cn"]},"values":["uid"],"overridable":false},{"schema":{"name":"accountSearchFilter","type":"java.lang.String","required":false,"order":11,"confidential":false,"defaultValues":[]},"values":["uid=*"],"overridable":false},{"schema":{"name":"groupObjectClasses","type":"[Ljava.lang.String;","required":false,"order":12,"confidential":false,"defaultValues":["top","groupOfUniqueNames"]},"values":[],"overridable":false},{"schema":{"name":"groupNameAttributes","type":"[Ljava.lang.String;","required":false,"order":13,"confidential":false,"defaultValues":["cn"]},"values":["cn"],"overridable":false},{"schema":{"name":"groupMemberAttribute","type":"java.lang.String","required":false,"order":14,"confidential":false,"defaultValues":["uniqueMember"]},"values":[],"overridable":false},{"schema":{"name":"maintainLdapGroupMembership","type":"boolean","required":false,"order":15,"confidential":false,"defaultValues":[false]},"values":["true"],"overridable":false},{"schema":{"name":"maintainPosixGroupMembership","type":"boolean","required":false,"order":16,"confidential":false,"defaultValues":[false]},"values":["false"],"overridable":false},{"schema":{"name":"addPrincipalToNewGroups","type":"boolean","required":false,"order":17,"confidential":false,"defaultValues":[false]},"values":["true"],"overridable":false},{"schema":{"name":"passwordHashAlgorithm","type":"java.lang.String","required":false,"order":18,"confidential":false,"defaultValues":[]},"values":["SHA"],"overridable":false},{"schema":{"name":"respectResourcePasswordPolicyChangeAfterReset","type":"boolean","required":false,"order":19,"confidential":false,"defaultValues":[false]},"values":["false"],"overridable":false},{"schema":{"name":"useVlvControls","type":"boolean","required":false,"order":20,"confidential":false,"defaultValues":[false]},"values":[],"overridable":false},{"schema":{"name":"vlvSortAttribute","type":"java.lang.String","required":false,"order":21,"confidential":false,"defaultValues":["uid"]},"values":[],"overridable":false},{"schema":{"name":"uidAttribute","type":"java.lang.String","required":false,"order":22,"confidential":false,"defaultValues":["entryUUID"]},"values":["cn"],"overridable":true},{"schema":{"name":"gidAttribute","type":"java.lang.String","required":false,"order":23,"confidential":false,"defaultValues":["entryUUID"]},"values":["cn"],"overridable":true},{"schema":{"name":"readSchema","type":"boolean","required":false,"order":23,"confidential":false,"defaultValues":[true]},"values":["true"],"overridable":false},{"schema":{"name":"baseContextsToSynchronize","type":"[Ljava.lang.String;","required":false,"order":24,"confidential":false,"defaultValues":[]},"values":["ou=people,o=isp","ou=groups,o=isp"],"overridable":false},{"schema":{"name":"objectClassesToSynchronize","type":"[Ljava.lang.String;","required":false,"order":25,"confidential":false,"defaultValues":["inetOrgPerson"]},"values":["inetOrgPerson","groupOfUniqueNames"],"overridable":false},{"schema":{"name":"attributesToSynchronize","type":"[Ljava.lang.String;","required":false,"order":26,"confidential":false,"defaultValues":[]},"values":[],"overridable":false},{"schema":{"name":"modifiersNamesToFilterOut","type":"[Ljava.lang.String;","required":false,"order":27,"confidential":false,"defaultValues":[]},"values":[],"overridable":false},{"schema":{"name":"accountSynchronizationFilter","type":"java.lang.String","required":false,"order":28,"confidential":false,"defaultValues":[]},"values":[],"overridable":false},{"schema":{"name":"changeLogBlockSize","type":"int","required":false,"order":29,"confidential":false,"defaultValues":[100]},"values":[100],"overridable":false},{"schema":{"name":"changeNumberAttribute","type":"java.lang.String","required":false,"order":30,"confidential":false,"defaultValues":["changeNumber"]},"values":["changeNumber"],"overridable":false},{"schema":{"name":"filterWithOrInsteadOfAnd","type":"boolean","required":false,"order":31,"confidential":false,"defaultValues":[false]},"values":["false"],"overridable":false},{"schema":{"name":"removeLogEntryObjectClassFromFilter","type":"boolean","required":false,"order":32,"confidential":false,"defaultValues":[true]},"values":["false"],"overridable":false},{"schema":{"name":"synchronizePasswords","type":"boolean","required":false,"order":33,"confidential":false,"defaultValues":[false]},"values":["false"],"overridable":false},{"schema":{"name":"passwordAttributeToSynchronize","type":"java.lang.String","required":false,"order":34,"confidential":false,"defaultValues":[]},"values":[],"overridable":false},{"schema":{"name":"passwordDecryptionKey","type":"org.identityconnectors.common.security.GuardedByteArray","required":false,"order":35,"confidential":true,"defaultValues":[]},"values":[],"overridable":false},{"schema":{"name":"passwordDecryptionInitializationVector","type":"org.identityconnectors.common.security.GuardedByteArray","required":false,"order":36,"confidential":true,"defaultValues":[]},"values":[],"overridable":false},{"schema":{"name":"statusManagementClass","type":"java.lang.String","required":false,"order":37,"confidential":false,"defaultValues":[]},"values":["net.tirasa.connid.bundles.ldap.commons.AttributeStatusManagement"],"overridable":false},{"schema":{"name":"retrievePasswordsWithSearch","type":"boolean","required":false,"order":38,"confidential":false,"defaultValues":[false]},"values":[],"overridable":false},{"schema":{"name":"dnAttribute","type":"java.lang.String","required":false,"order":39,"confidential":false,"defaultValues":["entryDN"]},"values":[],"overridable":false},{"schema":{"name":"groupSearchFilter","type":"java.lang.String","required":false,"order":40,"confidential":false,"defaultValues":[]},"values":[],"overridable":false},{"schema":{"name":"readTimeout","type":"long","required":false,"order":41,"confidential":false,"defaultValues":[0]},"values":[],"overridable":false},{"schema":{"name":"connectTimeout","type":"long","required":false,"order":42,"confidential":false,"defaultValues":[0]},"values":[],"overridable":false}]', 'file:/home/ilgrosso/work/syncope/fork/fit/core-reference/target/bundles/', '{"maxObjects":5,"minIdle":2,"maxIdle":3,"maxWait":10,"minEvictableIdleTimeMillis":5}', '1.5.10', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.conninstance VALUES ('a28abd9b-9f4a-4ef6-a7a8-d19ad2a8f29d', 'net.tirasa.connid.bundles.db', '["CREATE","UPDATE","SYNC","SEARCH"]', NULL, 'net.tirasa.connid.bundles.db.table.DatabaseTableConnector', 'H2-test2', '[{"schema":{"name":"disabledStatusValue","displayName":null,"helpMessage":null,"type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["false"]},{"schema":{"name":"user","displayName":null,"helpMessage":null,"type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["sa"]},{"schema":{"name":"keyColumn","displayName":null,"helpMessage":null,"type":"java.lang.String","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["id"]},{"schema":{"name":"cipherAlgorithm","displayName":null,"helpMessage":null,"type":"java.lang.String","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["SHA1"]},{"schema":{"name":"enabledStatusValue","displayName":null,"helpMessage":null,"type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["true"]},{"schema":{"name":"passwordColumn","displayName":null,"helpMessage":null,"type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["password"]},{"schema":{"name":"jdbcDriver","displayName":null,"helpMessage":null,"type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["org.h2.Driver"]},{"schema":{"name":"retrievePassword","displayName":null,"helpMessage":null,"type":"java.lang.Boolean","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["true"]},{"schema":{"name":"defaultStatusValue","displayName":null,"helpMessage":null,"type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["true"]},{"schema":{"name":"password","displayName":null,"helpMessage":null,"type":"org.identityconnectors.common.security.GuardedString","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["sa"]},{"schema":{"name":"statusColumn","displayName":null,"helpMessage":null,"type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["status"]},{"schema":{"name":"jdbcUrlTemplate","displayName":null,"helpMessage":null,"type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["jdbc:h2:tcp://localhost:9092/mem:testdb;DB_CLOSE_DELAY=-1"]},{"schema":{"name":"table","displayName":null,"helpMessage":null,"type":"java.lang.String","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":false,"values":["test2"]}]', 'connid://testconnectorserver@localhost:4554', NULL, '2.4.1', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.conninstance VALUES ('be24b061-019d-4e3e-baf0-0a6d0a45cb9c', 'net.tirasa.connid.bundles.db', '["CREATE","UPDATE","DELETE","SEARCH"]', NULL, 'net.tirasa.connid.bundles.db.table.DatabaseTableConnector', 'H2-testpull', '[{"schema":{"name":"changeLogColumn","displayName":"Change Log Column (Sync)","helpMessage":"=<b>Change Log Column</b><br>The change log column store the latest change time. Providing this value the Pull capabilities are activated.","type":"java.lang.String","required":false,"order":21,"confidential":false,"defaultValues":null},"overridable":false,"values":[]},{"schema":{"name":"nativeTimestamps","displayName":"Native Timestamps ","helpMessage":"<b>Native Timestamps</b><br>Select to retrieve Timestamp data type of the columns in java.sql.Timestamp format from the database table.","type":"boolean","required":false,"order":18,"confidential":false,"defaultValues":null},"overridable":false,"values":["false"]},{"schema":{"name":"cipherAlgorithm","displayName":"Password cipher algorithm (defaults to CLEARTEXT)","helpMessage":"Cipher algorithm used to encode password before to store it onto the database table.\nSpecify one of the values among CLEARTEXT,AES, MD5, SHA1, SHA256 or a custom implementation identified by its class name.","type":"java.lang.String","required":false,"order":24,"confidential":false,"defaultValues":null},"overridable":false,"values":[]},{"schema":{"name":"enabledStatusValue","displayName":"Enabled Status Value","helpMessage":"<b>Enabled Status Value</b><br>Enter the value for enabled status.","type":"java.lang.String","required":false,"order":12,"confidential":false,"defaultValues":null},"overridable":false,"values":[]},{"schema":{"name":"retrievePassword","displayName":"Retrieve password","helpMessage":"Specify if password must be retrieved by default.","type":"boolean","required":true,"order":27,"confidential":false,"defaultValues":null},"overridable":false,"values":["false"]},{"schema":{"name":"datasource","displayName":"Datasource Path","helpMessage":"<b>JDBC Data Source Name/Path</b><br>Enter the JDBC Data Source Name/Path to connect to the Oracle server. If specified, connector will only try to connect using Datasource and ignore other resource parameters specified.<br>the example value is: <CODE>jdbc/SampleDataSourceName</CODE>","type":"java.lang.String","required":false,"order":22,"confidential":false,"defaultValues":null},"overridable":false,"values":[]},{"schema":{"name":"allNative","displayName":"All native","helpMessage":"<b>All native</b><br>Select to retrieve all data type of the columns in a native format from the database table.","type":"boolean","required":false,"order":19,"confidential":false,"defaultValues":null},"overridable":false,"values":["false"]},{"schema":{"name":"user","displayName":"User","helpMessage":"<b>User</b><br>Enter the name of the mandatory Database user with permission to account table.","type":"java.lang.String","required":false,"order":4,"confidential":false,"defaultValues":null},"overridable":false,"values":["sa"]},{"schema":{"name":"pwdEncodeToLowerCase","displayName":"Force password encoding to lower case","helpMessage":"Force password encoding to lower case.","type":"boolean","required":false,"order":26,"confidential":false,"defaultValues":null},"overridable":false,"values":["false"]},{"schema":{"name":"jdbcUrlTemplate","displayName":"JDBC Connection URL","helpMessage":"<b>JDBC Connection URL</b><br>Specify the JDBC Driver Connection URL.<br> Oracle template is jdbc:oracle:thin:@[host]:[port(1521)]:[DB].<br>  MySQL template is jdbc:mysql://[host]:[port(3306)]/[db], for more info, read the JDBC driver documentation.<br>Could be empty if datasource is provided.","type":"java.lang.String","required":false,"order":15,"confidential":false,"defaultValues":null},"overridable":false,"values":["jdbc:h2:tcp://localhost:9092/mem:testdb;DB_CLOSE_DELAY=-1"]},{"schema":{"name":"keyColumn","displayName":"Key Column","helpMessage":"<b>Key Column</b><br>This mandatory column value will be used as the unique identifier for rows in the table.<br>","type":"java.lang.String","required":true,"order":8,"confidential":false,"defaultValues":null},"overridable":false,"values":["id"]},{"schema":{"name":"validConnectionQuery","displayName":"Validate Connection Query","helpMessage":"<b>Validate Connection Query</b><br>There can be specified the check connection alive query. If empty, default implementation will test it using the switch on/off the autocommit. Some select 1 from dummy table could be more efficient.","type":"java.lang.String","required":false,"order":20,"confidential":false,"defaultValues":null},"overridable":false,"values":[]},{"schema":{"name":"rethrowAllSQLExceptions","displayName":"Rethrow all SQLExceptions","helpMessage":"If this is not checked, SQL statements which throw SQLExceptions with a 0 ErrorCode will be have the exception caught and suppressed. Check it to have exceptions with 0 ErrorCodes rethrown.","type":"boolean","required":false,"order":17,"confidential":false,"defaultValues":null},"overridable":false,"values":["false"]},{"schema":{"name":"passwordColumn","displayName":"Password Column","helpMessage":"<b>Password Column</b><br>Enter the name of the column in the table that will hold the password values. If empty, no validation on resource and passwords are activated.","type":"java.lang.String","required":false,"order":9,"confidential":false,"defaultValues":null},"overridable":false,"values":[]},{"schema":{"name":"jndiProperties","displayName":"Initial JNDI Properties","helpMessage":"<b>Initial JNDI Properties</b><br>Could be empty or enter the JDBC JNDI Initial context factory, context provider in a format: key = value.","type":"[Ljava.lang.String;","required":false,"order":23,"confidential":false,"defaultValues":null},"overridable":false,"values":[]},{"schema":{"name":"password","displayName":"User Password","helpMessage":"<b>User Password</b><br>Enter a user account that has permission to access accounts table.","type":"org.identityconnectors.common.security.GuardedString","required":false,"order":5,"confidential":true,"defaultValues":null},"overridable":false,"values":["sa"]},{"schema":{"name":"host","displayName":"Host","helpMessage":"<b>Host</b><br>Enter the name of the host where the database is running.","type":"java.lang.String","required":false,"order":2,"confidential":false,"defaultValues":null},"overridable":false,"values":[]},{"schema":{"name":"port","displayName":"Port","helpMessage":"<b>TCP Port</b><br>Enter the port number the database server is listening on.","type":"java.lang.String","required":false,"order":3,"confidential":false,"defaultValues":null},"overridable":false,"values":[]},{"schema":{"name":"statusColumn","displayName":"Status Column","helpMessage":"<b>Status Column</b><br>Enter the name of the column in the table that will hold the status values. If empty enabled and disabled operation wont be performed.","type":"java.lang.String","required":false,"order":10,"confidential":false,"defaultValues":null},"overridable":false,"values":["status"]},{"schema":{"name":"pwdEncodeToUpperCase","displayName":"Force password encoding to upper case","helpMessage":"Force password encoding to upper case.","type":"boolean","required":false,"order":25,"confidential":false,"defaultValues":null},"overridable":false,"values":["false"]},{"schema":{"name":"enableEmptyString","displayName":"Enable writing empty string","helpMessage":"<b>Enable writing empty string</b><br>Select to enable support for writing an empty strings, instead of a NULL value, in character based columns defined as not-null in the table schema. This option does not influence the way strings are written for Oracle based tables. By default empty strings are written as a NULL value.","type":"boolean","required":false,"order":16,"confidential":false,"defaultValues":null},"overridable":false,"values":["false"]},{"schema":{"name":"database","displayName":"Database","helpMessage":"<b>Database</b><br>Enter the name of the database on the database server that contains the table.","type":"java.lang.String","required":false,"order":6,"confidential":false,"defaultValues":null},"overridable":false,"values":[]},{"schema":{"name":"defaultStatusValue","displayName":"Default Status Value","helpMessage":"<b>Default Status Value</b><br>Enter the value for status in case of status not specified.","type":"java.lang.String","required":false,"order":13,"confidential":false,"defaultValues":null},"overridable":false,"values":[]},{"schema":{"name":"table","displayName":"Table","helpMessage":"<b>Table</b><br>Enter the name of the table in the database that contains the accounts.","type":"java.lang.String","required":true,"order":7,"confidential":false,"defaultValues":null},"overridable":false,"values":["testpull"]},{"schema":{"name":"disabledStatusValue","displayName":"Disabled Status Value","helpMessage":"<b>Disabled Status Value</b><br>Enter the value for disabled status.","type":"java.lang.String","required":false,"order":11,"confidential":false,"defaultValues":null},"overridable":false,"values":[]},{"schema":{"name":"jdbcDriver","displayName":"JDBC Driver","helpMessage":"<b>JDBC Driver</b><br>Specify the JDBC Driver class name. Oracle is oracle.jdbc.driver.OracleDriver. MySQL is org.gjt.mm.mysql.Driver.<br>Could be empty if datasource is provided.","type":"java.lang.String","required":false,"order":14,"confidential":false,"defaultValues":null},"overridable":false,"values":["org.h2.Driver"]},{"schema":{"name":"quoting","displayName":"Name Quoting","helpMessage":"<b>Name Quoting</b><br>Select whether database column names for this resource should be quoted, and the quoting characters. By default, database column names are not quoted (None). For other selections (Single, Double, Back, or Brackets), column names will appear between single quotes, double quotes, back quotes, or brackets in the SQL generated to access the database.","type":"java.lang.String","required":false,"order":1,"confidential":false,"defaultValues":null},"overridable":false,"values":[]},{"schema":{"name":"cipherKey","displayName":"Password cipher key","helpMessage":"Specify key in case of reversible algorithm.","type":"java.lang.String","required":false,"order":25,"confidential":false,"defaultValues":null},"overridable":false,"values":[]}]', 'connid://testconnectorserver@localhost:4554', NULL, '2.4.1', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.conninstance VALUES ('a6d017fd-a705-4507-bb7c-6ab6a6745997', 'net.tirasa.connid.bundles.db', '["CREATE","UPDATE","UPDATE_DELTA","DELETE","SEARCH","SYNC"]', NULL, 'net.tirasa.connid.bundles.db.scriptedsql.ScriptedSQLConnector', 'Scripted SQL', '[{"schema":{"name":"updateScriptFileName","displayName":"updateScriptFileName","helpMessage":"updateScriptFileName","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":[]},"overridable":false,"values":["/home/ilgrosso/work/syncope/fork/fit/core-reference/target/test-classes/scriptedsql/UpdateScript.groovy"]},{"schema":{"name":"testScript","displayName":"testScript","helpMessage":"testScript","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":[""]},"overridable":false,"values":[]},{"schema":{"name":"host","displayName":"Host","helpMessage":"<b>Host</b><br/>Enter the name of the host where the database is running.","type":"java.lang.String","required":false,"order":2,"confidential":false,"defaultValues":["localhost"]},"overridable":false},{"schema":{"name":"port","displayName":"Port","helpMessage":"<b>TCP Port</b><br/>Enter the port number the database server is listening on.","type":"java.lang.String","required":false,"order":3,"confidential":false,"defaultValues":["3306"]},"overridable":false},{"schema":{"name":"database","displayName":"Database","helpMessage":"<b>Database</b><br/>Enter the name of the database on the database server that contains the table.","type":"java.lang.String","required":false,"order":6,"confidential":false,"defaultValues":[""]},"overridable":false},{"schema":{"name":"createScript","displayName":"createScript","helpMessage":"createScript","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":[""]},"overridable":false,"values":[]},{"schema":{"name":"jdbcUrlTemplate","displayName":"JDBC Connection URL","helpMessage":"<b>JDBC Connection URL</b><br/>Specify the JDBC Driver Connection URL.<br/> Oracle template is jdbc:oracle:thin:@[host]:[port(1521)]:[DB].<br/>  MySQL template is jdbc:mysql://[host]:[port(3306)]/[db], for more info, read the JDBC driver documentation.<br/>Could be empty if datasource is provided.","type":"java.lang.String","required":false,"order":11,"confidential":false,"defaultValues":["jdbc:mysql://%h:%p/%d"]},"overridable":false,"values":["jdbc:h2:tcp://localhost:9092/mem:testdb;DB_CLOSE_DELAY=-1"]},{"schema":{"name":"jndiProperties","displayName":"Initial JNDI Properties","helpMessage":"<b>Initial JNDI Properties</b><br/>Could be empty or enter the JDBC JNDI Initial context factory, context provider in a format: key = value.","type":"[Ljava.lang.String;","required":false,"order":21,"confidential":false,"defaultValues":[]},"overridable":false,"values":[]},{"schema":{"name":"enableEmptyString","displayName":"Enable writing empty string","helpMessage":"<b>Enable writing empty string</b><br/>Select to enable support for writing an empty strings, instead of a NULL value, in character based columns defined as not-null in the table schema. This option does not influence the way strings are written for Oracle based tables. By default empty strings are written as a NULL value.","type":"boolean","required":false,"order":12,"confidential":false,"defaultValues":[false]},"overridable":false,"values":["false"]},{"schema":{"name":"allNative","displayName":"All native","helpMessage":"<b>All native</b><br/>Select to retrieve all data type of the columns in a native format from the database table.","type":"boolean","required":false,"order":16,"confidential":false,"defaultValues":[false]},"overridable":false,"values":[false]},{"schema":{"name":"password","displayName":"User Password","helpMessage":"<b>User Password</b><br/>Enter a user account that has permission to access accounts table.","type":"org.identityconnectors.common.security.GuardedString","required":false,"order":5,"confidential":true,"defaultValues":[]},"overridable":false,"values":["sa"]},{"schema":{"name":"validConnectionQuery","displayName":"Validate Connection Query","helpMessage":"<b>Validate Connection Query</b><br/>There can be specified the check connection alive query. If empty, default implementation will test it using the switch on/off the autocommit. Some select 1 from dummy table could be more efficient.","type":"java.lang.String","required":false,"order":17,"confidential":false,"defaultValues":[]},"overridable":false,"values":[]},{"schema":{"name":"reloadScriptOnExecution","displayName":"reloadScriptOnExecution","helpMessage":"reloadScriptOnExecution","type":"boolean","required":false,"order":0,"confidential":false,"defaultValues":[false]},"overridable":false,"values":["true"]},{"schema":{"name":"schemaScriptFileName","displayName":"schemaScriptFileName","helpMessage":"schemaScriptFileName","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":[]},"overridable":true,"values":["/home/ilgrosso/work/syncope/fork/fit/core-reference/target/test-classes/scriptedsql/SchemaScript.groovy"]},{"schema":{"name":"jdbcDriver","displayName":"JDBC Driver","helpMessage":"<b>JDBC Driver</b><br/>Specify the JDBC Driver class name. Oracle is oracle.jdbc.driver.OracleDriver. MySQL is org.gjt.mm.mysql.Driver.<br/>Could be empty if datasource is provided.","type":"java.lang.String","required":false,"order":10,"confidential":false,"defaultValues":["com.mysql.jdbc.Driver"]},"overridable":false,"values":["org.h2.Driver"]},{"schema":{"name":"testScriptFileName","displayName":"testScriptFileName","helpMessage":"testScriptFileName","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":[]},"overridable":true,"values":["/home/ilgrosso/work/syncope/fork/fit/core-reference/target/test-classes/scriptedsql/TestScript.groovy"]},{"schema":{"name":"quoting","displayName":"Name Quoting","helpMessage":"<b>Name Quoting</b><br/>Select whether database column names for this resource should be quoted, and the quoting characters. By default, database column names are not quoted (None). For other selections (Single, Double, Back, or Brackets), column names will appear between single quotes, double quotes, back quotes, or brackets in the SQL generated to access the database.","type":"java.lang.String","required":false,"order":-1,"confidential":false,"defaultValues":[""]},"overridable":false,"values":[]},{"schema":{"name":"createScriptFileName","displayName":"createScriptFileName","helpMessage":"createScriptFileName","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":[]},"overridable":false,"values":["/home/ilgrosso/work/syncope/fork/fit/core-reference/target/test-classes/scriptedsql/CreateScript.groovy"]},{"schema":{"name":"clearTextPasswordToScript","displayName":"clearTextPasswordToScript","helpMessage":"clearTextPasswordToScript","type":"boolean","required":false,"order":0,"confidential":false,"defaultValues":[true]},"overridable":false,"values":["false"]},{"schema":{"name":"nativeTimestamps","displayName":"Native Timestamps","helpMessage":"<b>Native Timestamps</b><br/>Select to retrieve Timestamp data type of the columns in java.sql.Timestamp format from the database table.","type":"boolean","required":false,"order":15,"confidential":false,"defaultValues":[false]},"overridable":false,"values":[false]},{"schema":{"name":"syncScript","displayName":"syncScript","helpMessage":"syncScript","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":[""]},"overridable":false,"values":[]},{"schema":{"name":"autoCommit","displayName":"autoCommit","helpMessage":"autoCommit","type":"boolean","required":false,"order":0,"confidential":false,"defaultValues":[true]},"overridable":false,"values":[true]},{"schema":{"name":"scriptingLanguage","displayName":"scriptingLanguage","helpMessage":"scriptingLanguage","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":["GROOVY"]},"overridable":false,"values":["GROOVY"]},{"schema":{"name":"datasource","displayName":"Datasource Path","helpMessage":"<b>JDBC Data Source Name/Path</b><br/>Enter the JDBC Data Source Name/Path to connect to the Oracle server. If specified, connector will only try to connect using Datasource and ignore other resource parameters specified.<br/>the example value is: <CODE>jdbc/SampleDataSourceName</CODE>","type":"java.lang.String","required":false,"order":20,"confidential":false,"defaultValues":[""]},"overridable":false,"values":[]},{"schema":{"name":"deleteScript","displayName":"deleteScript","helpMessage":"deleteScript","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":[""]},"overridable":false,"values":[]},{"schema":{"name":"rethrowAllSQLExceptions","displayName":"Rethrow all SQLExceptions","helpMessage":"If this is not checked, SQL statements which throw SQLExceptions with a 0 ErrorCode will be have the exception caught and suppressed. Check it to have exceptions with 0 ErrorCodes rethrown.","type":"boolean","required":false,"order":14,"confidential":false,"defaultValues":[true]},"overridable":false,"values":[true]},{"schema":{"name":"syncScriptFileName","displayName":"syncScriptFileName","helpMessage":"syncScriptFileName","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":[]},"overridable":true,"values":["/home/ilgrosso/work/syncope/fork/fit/core-reference/target/test-classes/scriptedsql/SyncScript.groovy"]},{"schema":{"name":"updateScript","displayName":"updateScript","helpMessage":"updateScript","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":[""]},"overridable":false,"values":[]},{"schema":{"name":"user","displayName":"User","helpMessage":"<b>User</b><br/>Enter the name of the mandatory Database user with permission to account table.","type":"java.lang.String","required":false,"order":4,"confidential":false,"defaultValues":[""]},"overridable":false,"values":["sa"]},{"schema":{"name":"deleteScriptFileName","displayName":"deleteScriptFileName","helpMessage":"deleteScriptFileName","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":[]},"overridable":false,"values":["/home/ilgrosso/work/syncope/fork/fit/core-reference/target/test-classes/scriptedsql/DeleteScript.groovy"]},{"schema":{"name":"searchScriptFileName","displayName":"searchScriptFileName","helpMessage":"searchScriptFileName","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":[]},"overridable":true,"values":["/home/ilgrosso/work/syncope/fork/fit/core-reference/target/test-classes/scriptedsql/SearchScript.groovy"]},{"schema":{"name":"searchScript","displayName":"searchScript","helpMessage":"searchScript","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":[""]},"overridable":false,"values":[]}]', 'connid://testconnectorserver@localhost:4554', NULL, '2.4.1', '0679e069-7355-4b20-bd11-a5a0a5453c7c');
INSERT INTO public.conninstance VALUES ('44c02549-19c3-483c-8025-4919c3283c37', 'net.tirasa.connid.bundles.rest', '["AUTHENTICATE","CREATE","UPDATE","DELETE","SEARCH","SYNC"]', NULL, 'net.tirasa.connid.bundles.rest.RESTConnector', 'REST', '[{"schema":{"name":"authenticateScript","displayName":"authenticateScript","helpMessage":"authenticateScript","type":"java.lang.String","required":false,"order":6,"confidential":false,"defaultValues":[""]},"overridable":false,"values":[]},{"schema":{"name":"contentType","displayName":"contentType","helpMessage":"contentType","type":"java.lang.String","required":true,"order":-1,"confidential":false,"defaultValues":["application/json"]},"overridable":false,"values":["application/json"]},{"schema":{"name":"resolveUsernameScriptFileName","displayName":"resolveUsernameScriptFileName","helpMessage":"resolveUsernameScriptFileName","type":"java.lang.String","required":false,"order":15,"confidential":false,"defaultValues":[]},"overridable":false,"values":[]},{"schema":{"name":"createScriptFileName","displayName":"createScriptFileName","helpMessage":"createScriptFileName","type":"java.lang.String","required":false,"order":10,"confidential":false,"defaultValues":[]},"overridable":false,"values":["/home/ilgrosso/work/syncope/fork/fit/core-reference/target/test-classes/rest/CreateScript.groovy"]},{"schema":{"name":"username","displayName":"username","helpMessage":"username","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":[]},"overridable":false,"values":[]},{"schema":{"name":"updateScript","displayName":"updateScript","helpMessage":"updateScript","type":"java.lang.String","required":false,"order":4,"confidential":false,"defaultValues":[""]},"overridable":false,"values":[]},{"schema":{"name":"searchScript","displayName":"searchScript","helpMessage":"searchScript","type":"java.lang.String","required":false,"order":6,"confidential":false,"defaultValues":[""]},"overridable":false,"values":[]},{"schema":{"name":"clearTextPasswordToScript","displayName":"clearTextPasswordToScript","helpMessage":"clearTextPasswordToScript","type":"boolean","required":false,"order":1,"confidential":false,"defaultValues":[true]},"overridable":false,"values":[true]},{"schema":{"name":"syncScript","displayName":"syncScript","helpMessage":"syncScript","type":"java.lang.String","required":false,"order":7,"confidential":false,"defaultValues":[""]},"overridable":false,"values":[]},{"schema":{"name":"deleteScriptFileName","displayName":"deleteScriptFileName","helpMessage":"deleteScriptFileName","type":"java.lang.String","required":false,"order":12,"confidential":false,"defaultValues":[]},"overridable":false,"values":["/home/ilgrosso/work/syncope/fork/fit/core-reference/target/test-classes/rest/DeleteScript.groovy"]},{"schema":{"name":"resolveUsernameScript","displayName":"resolveUsernameScript","helpMessage":"resolveUsernameScript","type":"java.lang.String","required":false,"order":6,"confidential":false,"defaultValues":[""]},"overridable":false,"values":[]},{"schema":{"name":"searchScriptFileName","displayName":"searchScriptFileName","helpMessage":"searchScriptFileName","type":"java.lang.String","required":false,"order":13,"confidential":false,"defaultValues":[]},"overridable":false,"values":["/home/ilgrosso/work/syncope/fork/fit/core-reference/target/test-classes/rest/SearchScript.groovy"]},{"schema":{"name":"syncScriptFileName","displayName":"syncScriptFileName","helpMessage":"syncScriptFileName","type":"java.lang.String","required":false,"order":16,"confidential":false,"defaultValues":[]},"overridable":false,"values":["/home/ilgrosso/work/syncope/fork/fit/core-reference/target/test-classes/rest/SyncScript.groovy"]},{"schema":{"name":"schemaScriptFileName","displayName":"schemaScriptFileName","helpMessage":"schemaScriptFileName","type":"java.lang.String","required":false,"order":17,"confidential":false,"defaultValues":[]},"overridable":false,"values":["/home/ilgrosso/work/syncope/fork/fit/core-reference/target/test-classes/rest/SchemaScript.groovy"]},{"schema":{"name":"password","displayName":"password","helpMessage":"password","type":"org.identityconnectors.common.security.GuardedString","required":false,"order":1,"confidential":true,"defaultValues":[]},"overridable":false,"values":[]},{"schema":{"name":"updateScriptFileName","displayName":"updateScriptFileName","helpMessage":"updateScriptFileName","type":"java.lang.String","required":false,"order":11,"confidential":false,"defaultValues":[]},"overridable":false,"values":["/home/ilgrosso/work/syncope/fork/fit/core-reference/target/test-classes/rest/UpdateScript.groovy"]},{"schema":{"name":"testScriptFileName","displayName":"testScriptFileName","helpMessage":"testScriptFileName","type":"java.lang.String","required":false,"order":18,"confidential":false,"defaultValues":[]},"overridable":false,"values":["/home/ilgrosso/work/syncope/fork/fit/core-reference/target/test-classes/rest/TestScript.groovy"]},{"schema":{"name":"accept","displayName":"accept","helpMessage":"accept","type":"java.lang.String","required":true,"order":-2,"confidential":false,"defaultValues":["application/json"]},"overridable":false,"values":["application/json"]},{"schema":{"name":"baseAddress","displayName":"baseAddress","helpMessage":"baseAddress","type":"java.lang.String","required":true,"order":-3,"confidential":false,"defaultValues":[]},"overridable":false,"values":["http://localhost:9080/syncope-fit-build-tools/cxf/rest"]},{"schema":{"name":"authenticateScriptFileName","displayName":"authenticateScriptFileName","helpMessage":"authenticateScriptFileName","type":"java.lang.String","required":false,"order":14,"confidential":false,"defaultValues":[]},"overridable":false,"values":["/home/ilgrosso/work/syncope/fork/fit/core-reference/target/test-classes/rest/AuthenticateScript.groovy"]},{"schema":{"name":"deleteScript","displayName":"deleteScript","helpMessage":"deleteScript","type":"java.lang.String","required":false,"order":5,"confidential":false,"defaultValues":[""]},"overridable":false,"values":[]},{"schema":{"name":"schemaScript","displayName":"schemaScript","helpMessage":"schemaScript","type":"java.lang.String","required":false,"order":8,"confidential":false,"defaultValues":[""]},"overridable":false,"values":[]},{"schema":{"name":"createScript","displayName":"createScript","helpMessage":"createScript","type":"java.lang.String","required":false,"order":3,"confidential":false,"defaultValues":[""]},"overridable":false,"values":[]},{"schema":{"name":"scriptingLanguage","displayName":"scriptingLanguage","helpMessage":"scriptingLanguage","type":"java.lang.String","required":false,"order":0,"confidential":false,"defaultValues":["GROOVY"]},"overridable":false,"values":["GROOVY"]},{"schema":{"name":"testScript","displayName":"testScript","helpMessage":"testScript","type":"java.lang.String","required":false,"order":9,"confidential":false,"defaultValues":[""]},"overridable":false,"values":[]},{"schema":{"name":"reloadScriptOnExecution","displayName":"reloadScriptOnExecution","helpMessage":"reloadScriptOnExecution","type":"boolean","required":false,"order":2,"confidential":false,"defaultValues":[false]},"overridable":false,"values":[false]}]', 'connid://testconnectorserver@localhost:4554', NULL, '1.1.1', '0679e069-7355-4b20-bd11-a5a0a5453c7c');
INSERT INTO public.conninstance VALUES ('01938bdf-7ac6-7149-a103-3ec9e74cc824', 'net.tirasa.connid.bundles.kafka', '["DELETE","LIVE_SYNC","CREATE","UPDATE"]', NULL, 'net.tirasa.connid.bundles.kafka.KafkaConnector', 'Kafka', '[{"schema":{"name":"bootstrapServers","displayName":"Bootstrap servers","helpMessage":"A list of host/port pairs used to establish the initial connection to the Kafka cluster","type":"java.lang.String","required":true,"order":1,"confidential":false,"defaultValues":[]},"values":["localhost:19092"],"overridable":false},{"schema":{"name":"clientId","displayName":"Client id","helpMessage":"Client id for subscription","type":"java.lang.String","required":true,"order":2,"confidential":false,"defaultValues":[]},"values":["syncope"],"overridable":false},{"schema":{"name":"consumerGroupId","displayName":"Consumer Group id","helpMessage":"A unique string that identifies the consumer group this consumer belongs to.","type":"java.lang.String","required":true,"order":3,"confidential":false,"defaultValues":[]},"values":["syncope"],"overridable":false},{"schema":{"name":"autoOffsetReset","displayName":"Auto offset reset","helpMessage":"What to do when there is no initial offset in Kafka or if the current offset does not exist any more on the server (e.g. because that data has been deleted); defaults to earliest in order to make sure the producer sent all messages before the consumer starts.","type":"java.lang.String","required":true,"order":4,"confidential":false,"defaultValues":["earliest"]},"values":["earliest"],"overridable":false},{"schema":{"name":"valueSerializerClassName","displayName":"Value serializer class","helpMessage":"Serializer class for value that implements the org.apache.kafka.common.serialization.Serializer interface. Defaults to net.tirasa.connid.bundles.kafka.serialization.SyncDeltaSerializer","type":"java.lang.String","required":true,"order":5,"confidential":false,"defaultValues":["net.tirasa.connid.bundles.kafka.serialization.SyncDeltaSerializer"]},"values":["net.tirasa.connid.bundles.kafka.serialization.SyncDeltaSerializer"],"overridable":false},{"schema":{"name":"valueDeserializerClassName","displayName":"Value deserializer class","helpMessage":"Deserializer class for value that implements the org.apache.kafka.common.serialization.Deserializer interface. Defaults to org.apache.kafka.common.serialization.StringDeserializer","type":"java.lang.String","required":true,"order":6,"confidential":false,"defaultValues":["org.apache.kafka.common.serialization.StringDeserializer"]},"values":["org.apache.kafka.common.serialization.StringDeserializer"],"overridable":false},{"schema":{"name":"accountTopic","displayName":"Topic to publish to and being subscribed to for object class __ACCOUNT__","helpMessage":"A topic is similar to a folder in a filesystem, and the events are the files in that folder.","type":"java.lang.String","required":false,"order":7,"confidential":false,"defaultValues":["__ACCOUNT__"]},"values":["account-provisioning"],"overridable":false},{"schema":{"name":"groupTopic","displayName":"Topic to publish to and being subscribed to for object class __GROUP__","helpMessage":"A topic is similar to a folder in a filesystem, and the events are the files in that folder.","type":"java.lang.String","required":false,"order":8,"confidential":false,"defaultValues":["__GROUP__"]},"values":["group-provisioning"],"overridable":false},{"schema":{"name":"allTopic","displayName":"Topic to publish to and being subscribed to for object class __ALL__","helpMessage":"A topic is similar to a folder in a filesystem, and the events are the files in that folder.","type":"java.lang.String","required":false,"order":9,"confidential":false,"defaultValues":["__ALL__"]},"values":["__ALL__"],"overridable":false},{"schema":{"name":"consumerPollMillis","displayName":"The maximum number of milliseconds to block while polling","helpMessage":"Polling returns immediately if there are records available or if the position advances past control records; otherwise, it will await the passed timeout: if the timeout expires, an empty record set will be returned.","type":"long","required":false,"order":10,"confidential":false,"defaultValues":[100]},"values":["100"],"overridable":false}]', 'file:/home/ilgrosso/work/syncope/fork/fit/core-reference/target/bundles/', '{"maxObjects":5,"minIdle":2,"maxIdle":3,"maxWait":10,"minEvictableIdleTimeMillis":5}', '1.0.0', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');


--
-- TOC entry 4798 (class 0 OID 16583)
-- Dependencies: 244
-- Data for Name: delegation; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4799 (class 0 OID 16590)
-- Dependencies: 245
-- Data for Name: delegation_syncoperole; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4800 (class 0 OID 16593)
-- Dependencies: 246
-- Data for Name: derschema; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.derschema VALUES ('csvuserid', 'firstname + '','' + surname', 'csv');
INSERT INTO public.derschema VALUES ('cn', 'surname + '', '' + firstname', 'minimal user');
INSERT INTO public.derschema VALUES ('noschema', 'surname + '', '' + notfound', 'other');
INSERT INTO public.derschema VALUES ('info', 'username + '' - '' + creationDate + ''['' + failedLogins + '']''', 'minimal user');
INSERT INTO public.derschema VALUES ('rderiveddata', 'rderived_sx + ''-'' + rderived_dx', 'minimal group');
INSERT INTO public.derschema VALUES ('displayProperty', 'icon + '': '' + show', 'minimal group');
INSERT INTO public.derschema VALUES ('rderToBePropagated', 'rderived_sx + ''-'' + rderived_dx', 'minimal group');
INSERT INTO public.derschema VALUES ('rderivedschema', 'rderived_sx + ''-'' + rderived_dx', 'minimal group');
INSERT INTO public.derschema VALUES ('mderiveddata', 'mderived_sx + ''-'' + mderived_dx', NULL);
INSERT INTO public.derschema VALUES ('mderToBePropagated', 'mderived_sx + ''-'' + mderived_dx', 'generic membership');


--
-- TOC entry 4801 (class 0 OID 16600)
-- Dependencies: 247
-- Data for Name: dynrealm; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4924 (class 0 OID 18686)
-- Dependencies: 370
-- Data for Name: dynrealmmembers; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4802 (class 0 OID 16605)
-- Dependencies: 248
-- Data for Name: dynrealmmembership; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4925 (class 0 OID 18691)
-- Dependencies: 371
-- Data for Name: dynrolemembers; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4803 (class 0 OID 16612)
-- Dependencies: 249
-- Data for Name: externalresource; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.externalresource VALUES ('ws-target-resource-1', NULL, 'ALL', 'ALL', 0, NULL, NULL, 1, 'ALL', '[{"anyType":"USER","objectClass":"__ACCOUNT__","auxClasses":[],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":null,"items":[{"intAttrName":"username","extAttrName":"userId","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"email","extAttrName":"email","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"surname","extAttrName":"surname","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"password","extAttrName":"__PASSWORD__","connObjectKey":false,"password":true,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"surname","extAttrName":"fullname","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"ctype","extAttrName":"type","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"firstname","extAttrName":"name","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"NONE","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"username","extAttrName":"userId","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}}]', 'ALL', NULL, '88a7a819-dab5-46b4-9b90-0b9769eabdb8', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('ws-target-resource-2', NULL, 'FAILURES', 'NONE', 1, NULL, NULL, NULL, 'ALL', '[{"anyType":"USER","objectClass":"__ACCOUNT__","auxClasses":[],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":null,"items":[{"intAttrName":"fullname","extAttrName":"fullname","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"password","extAttrName":"__PASSWORD__","connObjectKey":false,"password":true,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"ctype","extAttrName":"type","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"surname","extAttrName":"surname","connObjectKey":false,"password":false,"mandatoryCondition":"type == ''F''","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"cn","extAttrName":"fullname","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"fullname","extAttrName":"fullname","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}}]', 'ALL', NULL, '88a7a819-dab5-46b4-9b90-0b9769eabdb8', '9454b0d7-2610-400a-be82-fc23cf553dd6', NULL, NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('ws-target-resource-timeout', NULL, 'FAILURES', 'NONE', 1, '[{"schema":{"name":"endpoint","displayName":null,"helpMessage":null,"type":"java.lang.String","required":true,"order":0,"confidential":false,"defaultValues":null},"overridable":true,"values":["http://localhost:9080/syncope-fit-build-tools/services/provisioning"]}]', NULL, 1, 'ALL', '[{"anyType":"USER","objectClass":"__ACCOUNT__","auxClasses":[],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":null,"items":[{"intAttrName":"fullname","extAttrName":"fullname","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"fullname","extAttrName":"fullname","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}}]', 'ALL', NULL, '5ffbb4ac-a8c3-4b44-b699-11b398a1ba08', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('ws-target-resource-list-mappings-1', NULL, 'ALL', 'ALL', 0, NULL, NULL, NULL, 'ALL', '[{"anyType":"USER","objectClass":"__ACCOUNT__","auxClasses":[],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":null,"items":[{"intAttrName":"email","extAttrName":"email","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"surname","extAttrName":"surname","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"email","extAttrName":"email","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}}]', 'ALL', NULL, '88a7a819-dab5-46b4-9b90-0b9769eabdb8', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('ws-target-resource-list-mappings-2', NULL, 'ALL', 'ALL', 1, NULL, NULL, NULL, 'ALL', NULL, 'ALL', NULL, '88a7a819-dab5-46b4-9b90-0b9769eabdb8', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('ws-target-resource-delete', NULL, 'ALL', 'ALL', 0, NULL, NULL, 2, 'ALL', '[{"anyType":"USER","objectClass":"__ACCOUNT__","auxClasses":[],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":null,"items":[{"intAttrName":"username","extAttrName":"userId","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"username","extAttrName":"userId","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}}]', 'ALL', NULL, '5ffbb4ac-a8c3-4b44-b699-11b398a1ba08', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('ws-target-resource-update', NULL, 'ALL', 'ALL', 1, NULL, NULL, NULL, 'ALL', '[{"anyType":"USER","objectClass":"__ACCOUNT__","auxClasses":[],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":null,"items":[{"intAttrName":"email","extAttrName":"email","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"userId","extAttrName":"userId","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"fullname","extAttrName":"test3","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"userId","extAttrName":"userId","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}}]', 'ALL', NULL, '88a7a819-dab5-46b4-9b90-0b9769eabdb8', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('resource-testdb', NULL, 'ALL', 'ALL', 1, NULL, NULL, NULL, 'ALL', '[{"anyType":"USER","objectClass":"__ACCOUNT__","auxClasses":[],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":null,"items":[{"intAttrName":"username","extAttrName":"ID","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"password","extAttrName":"__PASSWORD__","connObjectKey":false,"password":true,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"username","extAttrName":"ID","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}}]', 'ALL', '20ab5a8c-4b0c-432c-b957-f7fb9784d9f7', '5aa5b8be-7521-481a-9651-c557aea078c1', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('ws-target-resource-nopropagation4', NULL, 'ALL', 'ALL', 1, NULL, NULL, NULL, 'ALL', '[{"anyType":"USER","objectClass":"__ACCOUNT__","auxClasses":[],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":null,"items":[{"intAttrName":"fullname","extAttrName":"fullname","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"fullname","extAttrName":"fullname","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}}]', 'ALL', NULL, 'fcf9f2b0-f7d6-42c9-84a6-61b28255a42b', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('resource-testdb2', NULL, 'ALL', 'ALL', 1, NULL, NULL, NULL, 'ALL', '[{"anyType":"USER","objectClass":"__ACCOUNT__","auxClasses":[],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":null,"items":[{"intAttrName":"username","extAttrName":"ID","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"password","extAttrName":"__PASSWORD__","connObjectKey":false,"password":true,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"username","extAttrName":"ID","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}}]', 'ALL', NULL, 'a28abd9b-9f4a-4ef6-a7a8-d19ad2a8f29d', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('resource-csv', NULL, 'ALL', 'ALL', 0, NULL, NULL, NULL, 'ALL', '[{"anyType":"USER","objectClass":"__ACCOUNT__","auxClasses":["csv","generic membership","minimal group"],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":null,"items":[{"intAttrName":"username","extAttrName":"id","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"fullname","extAttrName":"id","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"password","extAttrName":"__PASSWORD__","connObjectKey":false,"password":true,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"firstname","extAttrName":"name","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"surname","extAttrName":"surname","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"userId","extAttrName":"email","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"PULL","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"email","extAttrName":"email","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"PULL","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"csvuserid","extAttrName":"__NAME__","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"groups[root].rderToBePropagated","extAttrName":"theirgroup","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"csvuserid","extAttrName":"__NAME__","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}}]', 'ALL', NULL, '6c2acf1b-b052-46f0-8c56-7a8ad6905edf', '880f8553-069b-4aed-9930-2cd53873f544', NULL, NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('ws-target-resource-update-resetsynctoken', NULL, 'ALL', 'ALL', 1, NULL, NULL, NULL, 'ALL', '[{"anyType":"USER","objectClass":"__ACCOUNT__","auxClasses":[],"syncToken":"{\"value\":null}","ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":null,"items":[{"intAttrName":"userId","extAttrName":"userId","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"fullname","extAttrName":"__PASSWORD__","connObjectKey":false,"password":true,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"userId","extAttrName":"userId","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}}]', 'ALL', NULL, '88a7a819-dab5-46b4-9b90-0b9769eabdb8', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('resource-ldap', NULL, 'ALL', 'ALL', 1, NULL, NULL, 1, 'ALL', '[{"anyType":"USER","objectClass":"__ACCOUNT__","auxClasses":["generic membership","minimal group"],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":"''uid='' + username + '',ou=people,o=isp''","items":[{"intAttrName":"username","extAttrName":"cn","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"password","extAttrName":"__PASSWORD__","connObjectKey":false,"password":true,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"surname","extAttrName":"sn","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"fullname","extAttrName":"cn","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"email","extAttrName":"mail","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"title","extAttrName":"title","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"postalAddress","extAttrName":"postalAddress","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"userId","extAttrName":"mail","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"obscure","extAttrName":"registeredAddress","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"photo","extAttrName":"jpegPhoto","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"username","extAttrName":"cn","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}},{"anyType":"GROUP","objectClass":"__GROUP__","auxClasses":[],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":"''cn='' + name + '',ou=groups,o=isp''","items":[{"intAttrName":"name","extAttrName":"cn","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"userOwner","extAttrName":"owner","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"title","extAttrName":"description","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"name","extAttrName":"cn","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}}]', 'ALL', NULL, '74141a3b-0762-4720-a4aa-fc3e374ef3ef', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('resource-ldap-orgunit', NULL, 'ALL', 'ALL', 1, '[{"schema":{"name":"uidAttribute","displayName":"Uid Attribute","helpMessage":"The name of the LDAP attribute which is mapped to the Uid attribute. Default is \"entryUUID\".","type":"java.lang.String","required":false,"order":21,"confidential":false,"defaultValues":["entryUUID"]},"overridable":true,"values":["l"]},{"schema":{"name":"baseContexts","displayName":"Base Contexts","helpMessage":"One or more starting points in the LDAP tree that will be used when searching the tree. Searches are performed when discovering users from the LDAP server or when looking for the groups of which a user is a member.","type":"[Ljava.lang.String;","required":true,"order":7,"confidential":false,"defaultValues":[]},"overridable":true,"values":["o=isp"]}]', '{"objectClass":"organizationalUnit","syncToken":null,"ignoreCaseMatch":false,"connObjectLink":"syncope:fullPath2Dn(fullPath, ''ou'') + '',o=isp''","items":[{"intAttrName":"fullPath","extAttrName":"l","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"name","extAttrName":"ou","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"ctype","extAttrName":"description","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"fullpath","extAttrName":"l","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}', 1, 'ALL', NULL, 'ALL', NULL, '74141a3b-0762-4720-a4aa-fc3e374ef3ef', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('ws-target-resource-nopropagation', NULL, 'ALL', 'ALL', 1, NULL, NULL, NULL, 'ALL', '[{"anyType":"USER","objectClass":"__ACCOUNT__","auxClasses":[],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":null,"items":[{"intAttrName":"fullname","extAttrName":"fullname","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"fullname","extAttrName":"fullname","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}}]', 'ALL', NULL, 'fcf9f2b0-f7d6-42c9-84a6-61b28255a42b', NULL, '986d1236-3ac5-4a19-810c-5ab21d79cba1', NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('ws-target-resource-nopropagation2', NULL, 'ALL', 'ALL', 1, NULL, NULL, NULL, 'ALL', '[{"anyType":"USER","objectClass":"__ACCOUNT__","auxClasses":[],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":null,"items":[{"intAttrName":"fullname","extAttrName":"fullname","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"fullname","extAttrName":"fullname","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}}]', 'ALL', NULL, 'fcf9f2b0-f7d6-42c9-84a6-61b28255a42b', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('ws-target-resource-nopropagation3', NULL, 'ALL', 'ALL', 1, NULL, NULL, NULL, 'ALL', '[{"anyType":"USER","objectClass":"__ACCOUNT__","auxClasses":[],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":null,"items":[{"intAttrName":"fullname","extAttrName":"fullname","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"fullname","extAttrName":"fullname","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"PROPAGATION","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}}]', 'ALL', NULL, 'fcf9f2b0-f7d6-42c9-84a6-61b28255a42b', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('resource-db-pull', NULL, 'ALL', 'ALL', 0, NULL, NULL, NULL, 'ALL', '[{"anyType":"USER","objectClass":"__ACCOUNT__","auxClasses":[],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":null,"items":[{"intAttrName":"email","extAttrName":"EMAIL","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"fullname","extAttrName":"SURNAME","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"PULL","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"firstname","extAttrName":"ID","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"surname","extAttrName":"SURNAME","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"username","extAttrName":"USERNAME","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"userId","extAttrName":"EMAIL","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"PULL","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"mustChangePassword","extAttrName":"MUSTCHANGEPASSWORD","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"PULL","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"firstname","extAttrName":"ID","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}}]', 'ALL', NULL, 'be24b061-019d-4e3e-baf0-0a6d0a45cb9c', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('resource-db-scripted', NULL, 'ALL', 'ALL', 0, NULL, NULL, NULL, 'ALL', '[{"anyType":"PRINTER","objectClass":"__PRINTER__","auxClasses":[],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":null,"items":[{"intAttrName":"key","extAttrName":"ID","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"name","extAttrName":"PRINTERNAME","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"location","extAttrName":"LOCATION","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"key","extAttrName":"ID","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}}]', 'ALL', NULL, 'a6d017fd-a705-4507-bb7c-6ab6a6745997', NULL, NULL, '89d322db-9878-420c-b49c-67be13df9a12', NULL, NULL);
INSERT INTO public.externalresource VALUES ('rest-target-resource', NULL, 'ALL', 'ALL', 1, NULL, NULL, 0, 'ALL', '[{"anyType":"USER","objectClass":"__ACCOUNT__","auxClasses":[],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"connObjectLink":null,"items":[{"intAttrName":"firstname","extAttrName":"firstName","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"key","extAttrName":"key","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"password","extAttrName":"__PASSWORD__","connObjectKey":false,"password":true,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"username","extAttrName":"username","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"email","extAttrName":"email","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"surname","extAttrName":"surname","connObjectKey":false,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectKeyItem":{"intAttrName":"key","extAttrName":"key","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}}}]', 'ALL', NULL, '44c02549-19c3-483c-8025-4919c3283c37', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.externalresource VALUES ('resource-kafka', NULL, 'ALL', 'ALL', 0, NULL, NULL, NULL, 'ALL', '[{"anyType":"USER","objectClass":"__ACCOUNT__","auxClasses":[],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"items":[{"intAttrName":"username","extAttrName":"__NAME__","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"password","extAttrName":"__PASSWORD__","connObjectKey":false,"password":true,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"email","extAttrName":"email","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"firstname","extAttrName":"givenName","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"surname","extAttrName":"lastName","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"userId","extAttrName":"email","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"PULL","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]},{"intAttrName":"fullname","extAttrName":"fullname","connObjectKey":false,"password":false,"mandatoryCondition":"false","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectLink":null}},{"anyType":"GROUP","objectClass":"__GROUP__","auxClasses":[],"syncToken":null,"ignoreCaseMatch":false,"uidOnCreate":null,"mapping":{"items":[{"intAttrName":"name","extAttrName":"__NAME__","connObjectKey":true,"password":false,"mandatoryCondition":"true","purpose":"BOTH","propagationJEXLTransformer":null,"pullJEXLTransformer":null,"transformers":[]}],"connObjectLink":null}}]', 'ALL', NULL, '01938bdf-7ac6-7149-a103-3ec9e74cc824', NULL, NULL, '01938c04-65a1-7944-884d-8a26b76bc01e', NULL, NULL);


--
-- TOC entry 4804 (class 0 OID 16619)
-- Dependencies: 250
-- Data for Name: externalresourcepropaction; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.externalresourcepropaction VALUES ('resource-testdb2', 'GenerateRandomPasswordPropagationActions');
INSERT INTO public.externalresourcepropaction VALUES ('resource-ldap', 'GenerateRandomPasswordPropagationActions');
INSERT INTO public.externalresourcepropaction VALUES ('resource-ldap', 'LDAPMembershipPropagationActions');


--
-- TOC entry 4805 (class 0 OID 16626)
-- Dependencies: 251
-- Data for Name: fiqlquery; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4892 (class 0 OID 18187)
-- Dependencies: 338
-- Data for Name: flw_ru_batch; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4893 (class 0 OID 18195)
-- Dependencies: 339
-- Data for Name: flw_ru_batch_part; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4806 (class 0 OID 16635)
-- Dependencies: 252
-- Data for Name: formpropertydef; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4807 (class 0 OID 16642)
-- Dependencies: 253
-- Data for Name: grelationship; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4808 (class 0 OID 16649)
-- Dependencies: 254
-- Data for Name: implementation; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.implementation VALUES ('DefaultPasswordRuleConf1', '{"_class":"org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf","maxLength":0,"minLength":8,"wordsNotPermitted":["notpermitted1","notpermitted2"]}', 'JAVA', 'PASSWORD_RULE');
INSERT INTO public.implementation VALUES ('DefaultPasswordRuleConf2', '{"_class":"org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf","maxLength":0,"minLength":10,"digit":1,"wordsNotPermitted":["notpermitted1","notpermitted2"]}', 'JAVA', 'PASSWORD_RULE');
INSERT INTO public.implementation VALUES ('DefaultAccountRuleConf1', '{"_class":"org.apache.syncope.common.lib.policy.DefaultAccountRuleConf","maxLength":0,"minLength":0,"pattern":null,"allUpperCase":false,"allLowerCase":false,"wordsNotPermitted":[],"schemasNotPermitted":[],"prefixesNotPermitted":[],"suffixesNotPermitted":[]}', 'JAVA', 'ACCOUNT_RULE');
INSERT INTO public.implementation VALUES ('DefaultAccountRuleConf2', '{"_class":"org.apache.syncope.common.lib.policy.DefaultAccountRuleConf","maxLength":0,"minLength":4,"pattern":null,"allUpperCase":false,"allLowerCase":false,"wordsNotPermitted":[],"schemasNotPermitted":[],"prefixesNotPermitted":["notpermitted1","notpermitted2"],"suffixesNotPermitted":[]}', 'JAVA', 'ACCOUNT_RULE');
INSERT INTO public.implementation VALUES ('DefaultPasswordRuleConf3', '{"_class":"org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf","maxLength":0,"minLength":10,"special":1,"specialChars":["@","!"],"digit":1,"lowercase":1,"uppercase":1,"wordsNotPermitted":["notpermitted1","notpermitted2"]}', 'JAVA', 'PASSWORD_RULE');
INSERT INTO public.implementation VALUES ('EmailAddressValidator', 'org.apache.syncope.core.persistence.common.attrvalue.EmailAddressValidator', 'JAVA', 'ATTR_VALUE_VALIDATOR');
INSERT INTO public.implementation VALUES ('TestDropdownValueProvider', 'import java.util.List
import org.apache.syncope.common.lib.to.AttributableTO
import org.apache.syncope.core.persistence.api.attrvalue.DropdownValueProvider

class TestDropdownValueProvider implements DropdownValueProvider {
List<String> getChoices(AttributableTO attributableTO) {
return ["A", "B"]
}
}', 'GROOVY', 'DROPDOWN_VALUE_PROVIDER');
INSERT INTO public.implementation VALUES ('BinaryValidator', 'org.apache.syncope.core.persistence.common.attrvalue.BinaryValidator', 'JAVA', 'ATTR_VALUE_VALIDATOR');
INSERT INTO public.implementation VALUES ('TestInboundCorrelationRule', '{"_class":"org.apache.syncope.common.lib.policy.DefaultInboundCorrelationRuleConf","name":"org.apache.syncope.common.lib.policy.DefaultInboundCorrelationRuleConf","schemas":["username","firstname"]}', 'JAVA', 'INBOUND_CORRELATION_RULE');
INSERT INTO public.implementation VALUES ('TestPushCorrelationRule', '{"_class":"org.apache.syncope.common.lib.policy.DefaultPushCorrelationRuleConf","name":"org.apache.syncope.common.lib.policy.DefaultPushCorrelationRuleConf","schemas":["surname"]}', 'JAVA', 'PUSH_CORRELATION_RULE');
INSERT INTO public.implementation VALUES ('GenerateRandomPasswordPropagationActions', 'org.apache.syncope.core.provisioning.java.propagation.GenerateRandomPasswordPropagationActions', 'JAVA', 'PROPAGATION_ACTIONS');
INSERT INTO public.implementation VALUES ('LDAPMembershipPropagationActions', 'org.apache.syncope.core.provisioning.java.propagation.LDAPMembershipPropagationActions', 'JAVA', 'PROPAGATION_ACTIONS');
INSERT INTO public.implementation VALUES ('MacroJobDelegate', 'org.apache.syncope.core.provisioning.java.job.MacroJobDelegate', 'JAVA', 'TASKJOB_DELEGATE');
INSERT INTO public.implementation VALUES ('LiveSyncJobDelegate', 'org.apache.syncope.core.provisioning.java.pushpull.LiveSyncJobDelegate', 'JAVA', 'TASKJOB_DELEGATE');
INSERT INTO public.implementation VALUES ('PullJobDelegate', 'org.apache.syncope.core.provisioning.java.pushpull.PullJobDelegate', 'JAVA', 'TASKJOB_DELEGATE');
INSERT INTO public.implementation VALUES ('PushJobDelegate', 'org.apache.syncope.core.provisioning.java.pushpull.PushJobDelegate', 'JAVA', 'TASKJOB_DELEGATE');
INSERT INTO public.implementation VALUES ('GroupMemberProvisionTaskJobDelegate', 'org.apache.syncope.core.provisioning.java.job.GroupMemberProvisionTaskJobDelegate', 'JAVA', 'TASKJOB_DELEGATE');
INSERT INTO public.implementation VALUES ('TestSampleJobDelegate', 'org.apache.syncope.fit.core.reference.TestSampleJobDelegate', 'JAVA', 'TASKJOB_DELEGATE');
INSERT INTO public.implementation VALUES ('ExpiredAccessTokenCleanup', 'org.apache.syncope.core.provisioning.java.job.ExpiredAccessTokenCleanup', 'JAVA', 'TASKJOB_DELEGATE');
INSERT INTO public.implementation VALUES ('LDAPMembershipPullActions', 'org.apache.syncope.core.provisioning.java.pushpull.LDAPMembershipPullActions', 'JAVA', 'INBOUND_ACTIONS');
INSERT INTO public.implementation VALUES ('ExpiredBatchCleanup', 'org.apache.syncope.core.provisioning.java.job.ExpiredBatchCleanup', 'JAVA', 'TASKJOB_DELEGATE');
INSERT INTO public.implementation VALUES ('SampleReportJobDelegate', '{"_class":"org.apache.syncope.fit.core.reference.SampleReportConf","stringValue":"a string","intValue":1,"longValue":45,"floatValue":1.2,"doubleValue":2.1}', 'JAVA', 'REPORT_DELEGATE');


--
-- TOC entry 4809 (class 0 OID 16656)
-- Dependencies: 255
-- Data for Name: inboundcorrelationruleentity; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.inboundcorrelationruleentity VALUES ('10e3d196-7486-4c88-aefd-59e40d93a0c1', '880f8553-069b-4aed-9930-2cd53873f544', 'USER', 'TestInboundCorrelationRule');


--
-- TOC entry 4810 (class 0 OID 16665)
-- Dependencies: 256
-- Data for Name: inboundpolicy; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.inboundpolicy VALUES ('66691e96-285f-4464-bc19-e68384ea4c85', 'a pull policy', 'IGNORE');
INSERT INTO public.inboundpolicy VALUES ('880f8553-069b-4aed-9930-2cd53873f544', 'another pull policy', 'ALL');
INSERT INTO public.inboundpolicy VALUES ('4ad10d94-e002-4b3f-b771-16089cc71da9', 'pull policy 1', 'IGNORE');
INSERT INTO public.inboundpolicy VALUES ('9454b0d7-2610-400a-be82-fc23cf553dd6', 'pull policy for java rule', 'IGNORE');


--
-- TOC entry 4811 (class 0 OID 16670)
-- Dependencies: 257
-- Data for Name: jobstatus; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4812 (class 0 OID 16677)
-- Dependencies: 258
-- Data for Name: linkedaccount; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4813 (class 0 OID 16686)
-- Dependencies: 259
-- Data for Name: livesynctask; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4814 (class 0 OID 16697)
-- Dependencies: 260
-- Data for Name: livesynctaskaction; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4815 (class 0 OID 16702)
-- Dependencies: 261
-- Data for Name: livesynctaskexec; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4816 (class 0 OID 16709)
-- Dependencies: 262
-- Data for Name: macrotask; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4817 (class 0 OID 16718)
-- Dependencies: 263
-- Data for Name: macrotaskcommand; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4818 (class 0 OID 16725)
-- Dependencies: 264
-- Data for Name: macrotaskexec; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4819 (class 0 OID 16732)
-- Dependencies: 265
-- Data for Name: mailtemplate; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.mailtemplate VALUES ('requestPasswordReset', '<html> <body> <p>Hi, a password reset was requested for ${user.getUsername()}.</p>  <p>In order to complete this request, you need to visit this  <a href="http://localhost:9080/syncope-enduser/confirmpasswordreset?token=${input.get(0).replaceAll('' '', ''%20'')}">link</a></p>.  <p>If you did not request this reset, just ignore the present e-mail.</p>  <p>Best regards.</p> </body> </html>', 'Hi, a password reset was requested for ${user.getUsername()}.  In order to complete this request, you need to visit this link:  http://localhost:9080/syncope-enduser/confirmpasswordreset?token=${input.get(0).replaceAll('' '', ''%20'')}  If you did not request this reset, just ignore the present e-mail.  Best regards.');
INSERT INTO public.mailtemplate VALUES ('confirmPasswordReset', '<html> <body> <p>Hi,<br/> we are happy to inform you that the password request was successfully executed for your account.</p>  <p>Best regards.</p> </body> </html>', 'Hi, we are happy to inform you that the password request was successfully executed for your account.  Best regards.');
INSERT INTO public.mailtemplate VALUES ('test', NULL, NULL);
INSERT INTO public.mailtemplate VALUES ('optin', '<html> <body> <h3>Hi ${user.getPlainAttr("firstname").get().values[0]} ${user.getPlainAttr("surname").get().values[0]}, welcome to Syncope!</h3>  <p>    Your username is ${user.username}.<br/>    Your email address is ${user.getPlainAttr("email").get().values[0]}.    Your email address inside a <a href="http://localhost/?email=${user.getPlainAttr("email").get().values[0].replace(''@'', ''%40'')}">link</a>. </p>  <p>     This message was sent to the following recipients: <ul>
 $$ for (recipient: recipients) {
Na   <li>${recipient.getPlainAttr("email").get().values[0]}</li>
 $$ }
 </ul>
  because one of the following events occurred: <ul>
 $$ for (event: events) {
   <li>${event}</li>
 $$ }
 </ul>
 </p> 
 $$ if (!empty(user.memberships)) {
 You have been provided with the following groups:
 <ul>
 $$ for(membership : user.memberships) {
   <li>${membership.groupName}</li>
 $$ }
 </ul>
 $$ }
 </body> </html>', 'Hi ${user.getPlainAttr("firstname").get().values[0]} ${user.getPlainAttr("surname").get().values[0]}, welcome to Syncope!  Your username is ${user.username}. Your email address is ${user.getPlainAttr("email").get().values[0]}. Your email address inside a link: http://localhost/?email=${user.getPlainAttr("email").get().values[0].replace(''@'', ''%40'')}  This message was sent to the following recipients:
 $$ for (recipient: recipients) {
   * ${recipient.getPlainAttr("email").get().values[0]}
 $$ }
 
 because one of the following events occurred:
 $$ for (event: events) {
   * ${event}
 $$ }
 
 $$ if (!empty(user.memberships)) {
 You have been provided with the following groups:
 $$ for(membership : user.memberships) {
   * ${membership.groupName}
 $$ }
 $$ }
');


--
-- TOC entry 4820 (class 0 OID 16739)
-- Dependencies: 266
-- Data for Name: networkservice; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.networkservice VALUES ('019c5206-7e44-771c-b6d4-ce1dae336b4d', 'http://localhost:9080/syncope/rest/', 'CORE');


--
-- TOC entry 4821 (class 0 OID 16746)
-- Dependencies: 267
-- Data for Name: notification; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.notification VALUES ('e00945b5-1184-4d43-8e45-4318a8dcdfd4', 1, '["[CUSTOM]:[]:[]:[requestPasswordReset]:[SUCCESS]"]', 'email', NULL, 1, 'admin@syncope.apache.org', NULL, 'Password Reset request', 'FAILURES', NULL, 'requestPasswordReset');
INSERT INTO public.notification VALUES ('bef0c250-e8a7-4848-bb63-2564fc409ce2', 1, '["[CUSTOM]:[]:[]:[confirmPasswordReset]:[SUCCESS]"]', 'email', NULL, 1, 'admin@syncope.apache.org', NULL, 'Password Reset successful', 'FAILURES', NULL, 'confirmPasswordReset');
INSERT INTO public.notification VALUES ('9e2b911c-25de-4c77-bcea-b86ed9451050', 1, '["[CUSTOM]:[]:[]:[unexisting1]:[FAILURE]", "[CUSTOM]:[]:[]:[unexisting2]:[SUCCESS]"]', 'email', '$groups==7', 0, 'test@syncope.apache.org', NULL, 'Test subject', 'FAILURES', NULL, 'test');


--
-- TOC entry 4822 (class 0 OID 16753)
-- Dependencies: 268
-- Data for Name: notificationtask; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.notificationtask VALUES ('e1e520f0-2cbd-4e11-9a89-ea58a0f957e7', NULL, NULL, 1, 'NOTIFICATION-81', '["recipient@prova.org"]', 'admin@prova.org', 'Notification for SYNCOPE-81', 'NOTIFICATION-81', 'ALL', 'e00945b5-1184-4d43-8e45-4318a8dcdfd4');


--
-- TOC entry 4823 (class 0 OID 16760)
-- Dependencies: 269
-- Data for Name: notificationtaskexec; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.notificationtaskexec VALUES ('019c5206-c00e-77d2-9acc-c07459bdb92b', '2026-02-12 13:25:00.216099+00', 'admin', 'FROM: admin@prova.org
TO: recipient@prova.org
SUBJECT: Notification for SYNCOPE-81

NOTIFICATION-81

NOTIFICATION-81
', '2026-02-12 13:25:00.046096+00', 'SENT', 'e1e520f0-2cbd-4e11-9a89-ea58a0f957e7');


--
-- TOC entry 4824 (class 0 OID 16767)
-- Dependencies: 270
-- Data for Name: oidcjwks; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4825 (class 0 OID 16774)
-- Dependencies: 271
-- Data for Name: oidcprovider; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4826 (class 0 OID 16787)
-- Dependencies: 272
-- Data for Name: oidcprovideraction; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4827 (class 0 OID 16790)
-- Dependencies: 273
-- Data for Name: oidcrpclientapp; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4828 (class 0 OID 16803)
-- Dependencies: 274
-- Data for Name: oidcusertemplate; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4829 (class 0 OID 16812)
-- Dependencies: 275
-- Data for Name: passwordpolicy; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.passwordpolicy VALUES ('ce93fcda-dc3a-4369-a7b0-a6108c261c85', 'a password policy', 1, 1);
INSERT INTO public.passwordpolicy VALUES ('986d1236-3ac5-4a19-810c-5ab21d79cba1', 'sample password policy', 1, 0);
INSERT INTO public.passwordpolicy VALUES ('55e5de0b-c79c-4e66-adda-251b6fb8579a', 'sample password policy', 0, 0);


--
-- TOC entry 4830 (class 0 OID 16817)
-- Dependencies: 276
-- Data for Name: passwordpolicyrule; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.passwordpolicyrule VALUES ('ce93fcda-dc3a-4369-a7b0-a6108c261c85', 'DefaultPasswordRuleConf1');
INSERT INTO public.passwordpolicyrule VALUES ('986d1236-3ac5-4a19-810c-5ab21d79cba1', 'DefaultPasswordRuleConf2');
INSERT INTO public.passwordpolicyrule VALUES ('55e5de0b-c79c-4e66-adda-251b6fb8579a', 'DefaultPasswordRuleConf3');


--
-- TOC entry 4831 (class 0 OID 16822)
-- Dependencies: 277
-- Data for Name: plainschema; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.plainschema VALUES ('fullname', NULL, NULL, NULL, 'true', NULL, 0, 0, NULL, 'String', 1, 'minimal user', NULL, NULL);
INSERT INTO public.plainschema VALUES ('userId', NULL, NULL, NULL, 'true', NULL, 0, 0, NULL, 'String', 1, 'minimal user', NULL, 'EmailAddressValidator');
INSERT INTO public.plainschema VALUES ('loginDate', NULL, 'yyyy-MM-dd', NULL, 'false', NULL, 1, 0, NULL, 'Date', 0, 'other', NULL, NULL);
INSERT INTO public.plainschema VALUES ('firstname', NULL, NULL, NULL, 'false', NULL, 0, 0, NULL, 'String', 0, 'minimal user', NULL, NULL);
INSERT INTO public.plainschema VALUES ('surname', NULL, NULL, NULL, 'true', NULL, 0, 0, NULL, 'String', 0, 'minimal user', NULL, NULL);
INSERT INTO public.plainschema VALUES ('ctype', NULL, NULL, NULL, 'false', NULL, 0, 0, NULL, 'String', 0, 'other', NULL, NULL);
INSERT INTO public.plainschema VALUES ('email', NULL, NULL, NULL, 'false', NULL, 0, 0, NULL, 'String', 0, 'minimal user', NULL, 'EmailAddressValidator');
INSERT INTO public.plainschema VALUES ('activationDate', NULL, 'yyyy-MM-dd''T''HH:mm:ss.SSSZ', NULL, 'false', NULL, 0, 0, NULL, 'Date', 0, 'other', NULL, NULL);
INSERT INTO public.plainschema VALUES ('uselessReadonly', NULL, NULL, NULL, 'false', NULL, 0, 1, NULL, 'String', 0, 'other', NULL, NULL);
INSERT INTO public.plainschema VALUES ('cool', NULL, NULL, NULL, 'false', NULL, 0, 0, NULL, 'Boolean', 0, 'other', NULL, NULL);
INSERT INTO public.plainschema VALUES ('gender', NULL, NULL, '{"M":"Male","F":"Female"}', 'false', NULL, 0, 0, NULL, 'Enum', 0, 'other', NULL, NULL);
INSERT INTO public.plainschema VALUES ('dd', NULL, NULL, NULL, 'false', NULL, 0, 0, NULL, 'Dropdown', 0, 'other', 'TestDropdownValueProvider', NULL);
INSERT INTO public.plainschema VALUES ('aLong', NULL, NULL, NULL, 'false', NULL, 0, 0, NULL, 'Long', 0, 'other', NULL, NULL);
INSERT INTO public.plainschema VALUES ('makeItDouble', NULL, NULL, NULL, 'false', NULL, 0, 0, NULL, 'Long', 0, 'other', NULL, NULL);
INSERT INTO public.plainschema VALUES ('obscure', 'SHA', NULL, NULL, 'false', NULL, 0, 0, '7abcdefghilmnopqrstuvz9#', 'Encrypted', 0, 'other', NULL, NULL);
INSERT INTO public.plainschema VALUES ('photo', NULL, NULL, NULL, 'false', 'image/jpeg', 0, 0, NULL, 'Binary', 0, 'other', NULL, NULL);
INSERT INTO public.plainschema VALUES ('icon', NULL, NULL, NULL, 'false', NULL, 0, 0, NULL, 'String', 0, 'minimal group', NULL, NULL);
INSERT INTO public.plainschema VALUES ('show', NULL, NULL, NULL, 'false', NULL, 0, 0, NULL, 'Boolean', 0, 'minimal group', NULL, NULL);
INSERT INTO public.plainschema VALUES ('rderived_sx', NULL, NULL, NULL, 'false', NULL, 0, 0, NULL, 'String', 0, 'minimal group', NULL, NULL);
INSERT INTO public.plainschema VALUES ('rderived_dx', NULL, NULL, NULL, 'false', NULL, 0, 0, NULL, 'String', 0, 'minimal group', NULL, NULL);
INSERT INTO public.plainschema VALUES ('title', NULL, NULL, NULL, 'false', NULL, 1, 0, NULL, 'String', 0, 'minimal group', NULL, NULL);
INSERT INTO public.plainschema VALUES ('originalName', NULL, NULL, NULL, 'false', NULL, 0, 0, NULL, 'String', 1, 'minimal group', NULL, NULL);
INSERT INTO public.plainschema VALUES ('subscriptionDate', NULL, 'yyyy-MM-dd''T''HH:mm:ss.SSSZ', NULL, 'false', NULL, 0, 0, NULL, 'Date', 0, 'generic membership', NULL, NULL);
INSERT INTO public.plainschema VALUES ('mderived_sx', NULL, NULL, NULL, 'false', NULL, 0, 0, NULL, 'String', 0, 'generic membership', NULL, NULL);
INSERT INTO public.plainschema VALUES ('mderived_dx', NULL, NULL, NULL, 'false', NULL, 0, 0, NULL, 'String', 0, 'generic membership', NULL, NULL);
INSERT INTO public.plainschema VALUES ('postalAddress', NULL, NULL, NULL, 'false', NULL, 0, 0, NULL, 'String', 0, 'generic membership', NULL, NULL);
INSERT INTO public.plainschema VALUES ('model', NULL, NULL, NULL, 'false', NULL, 0, 0, NULL, 'String', 0, 'minimal printer', NULL, NULL);
INSERT INTO public.plainschema VALUES ('location', NULL, NULL, NULL, 'false', NULL, 0, 0, NULL, 'String', 0, 'minimal printer', NULL, NULL);


--
-- TOC entry 4832 (class 0 OID 16829)
-- Dependencies: 278
-- Data for Name: propagationpolicy; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.propagationpolicy VALUES ('89d322db-9878-420c-b49c-67be13df9a12', 'sample propagation policy', '10000', 'FIXED', 1, 5, 0);
INSERT INTO public.propagationpolicy VALUES ('01938c04-65a1-7944-884d-8a26b76bc01e', 'queue propagation policy', '1000', 'FIXED', 0, 3, 0);


--
-- TOC entry 4833 (class 0 OID 16836)
-- Dependencies: 279
-- Data for Name: propagationtask; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.propagationtask VALUES ('1e697572-b896-484c-ae7f-0c8f63fcbc6c', NULL, 'USER', NULL, '1417acbe-cbf6-4277-9372-e75e04f97000', '__ACCOUNT__', NULL, 'UPDATE', '{"attributes":[{"name":"__PASSWORD__","value":[{"readOnly":false,"disposed":false,"encryptedBytes":"m9nh2US0Sa6m+cXccCq0Xw==","base64SHA1Hash":"GFJ69qfjxEOdrmt+9q+0Cw2uz60="}]},{"name":"__NAME__","value":["userId"],"nameValue":"userId"},{"name":"fullname","value":["fullname"]},{"name":"type","value":["type"]}]}', 'ws-target-resource-2');
INSERT INTO public.propagationtask VALUES ('b8870cfb-3c1e-4fc4-abcb-2559826232e6', NULL, 'USER', NULL, '1417acbe-cbf6-4277-9372-e75e04f97000', '__ACCOUNT__', NULL, 'CREATE', '{"attributes":[{"name":"__PASSWORD__","value":[{"readOnly":false,"disposed":false,"encryptedBytes":"m9nh2US0Sa6m+cXccCq0Xw==","base64SHA1Hash":"GFJ69qfjxEOdrmt+9q+0Cw2uz60="}]},{"name":"__NAME__","value":["userId"],"nameValue":"userId"},{"name":"fullname","value":["fullname"]},{"name":"type","value":["type"]}]}', 'ws-target-resource-2');
INSERT INTO public.propagationtask VALUES ('316285cc-ae52-4ea2-a33b-7355e189ac3f', NULL, 'USER', NULL, '1417acbe-cbf6-4277-9372-e75e04f97000', '__ACCOUNT__', NULL, 'DELETE', '{"attributes":[{"name":"__PASSWORD__","value":[{"readOnly":false,"disposed":false,"encryptedBytes":"m9nh2US0Sa6m+cXccCq0Xw==","base64SHA1Hash":"GFJ69qfjxEOdrmt+9q+0Cw2uz60="}]},{"name":"__NAME__","value":["userId"],"nameValue":"userId"},{"name":"type","value":["type"]}]}', 'ws-target-resource-2');
INSERT INTO public.propagationtask VALUES ('025c956d-ea88-4bd7-9e44-2f35e0aa7055', NULL, 'USER', NULL, '1417acbe-cbf6-4277-9372-e75e04f97000', '__ACCOUNT__', NULL, 'UPDATE', '{"attributes":[{"name":"__PASSWORD__","value":[{"readOnly":false,"disposed":false,"encryptedBytes":"m9nh2US0Sa6m+cXccCq0Xw==","base64SHA1Hash":"GFJ69qfjxEOdrmt+9q+0Cw2uz60="}]},{"name":"__NAME__","value":["userId"],"nameValue":"userId"},{"name":"fullname","value":["fullname"]},{"name":"type","value":["type"]}]}', 'ws-target-resource-1');
INSERT INTO public.propagationtask VALUES ('d6c2d6d3-6329-44c1-9187-f1469ead1cfa', NULL, 'USER', NULL, '1417acbe-cbf6-4277-9372-e75e04f97000', '__ACCOUNT__', NULL, 'UPDATE', '{"attributes":[{"name":"__PASSWORD__","value":[{"readOnly":false,"disposed":false,"encryptedBytes":"m9nh2US0Sa6m+cXccCq0Xw==","base64SHA1Hash":"GFJ69qfjxEOdrmt+9q+0Cw2uz60="}]},{"name":"__NAME__","value":["userId"],"nameValue":"userId"},{"name":"fullname","value":["fullname"]},{"name":"type","value":["type"]}]}', 'ws-target-resource-nopropagation');
INSERT INTO public.propagationtask VALUES ('0f618183-17ce-48bc-80bc-cc535f38983a', NULL, 'USER', NULL, '1417acbe-cbf6-4277-9372-e75e04f97000', '__ACCOUNT__', NULL, 'CREATE', '{"attributes":[{"name":"__PASSWORD__","value":[{"readOnly":false,"disposed":false,"encryptedBytes":"m9nh2US0Sa6m+cXccCq0Xw==","base64SHA1Hash":"GFJ69qfjxEOdrmt+9q+0Cw2uz60="}]},{"name":"__NAME__","value":["userId"],"nameValue":"userId"},{"name":"fullname","value":["fullname"]},{"name":"type","value":["type"]}]}', 'resource-testdb');


--
-- TOC entry 4834 (class 0 OID 16843)
-- Dependencies: 280
-- Data for Name: propagationtaskexec; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.propagationtaskexec VALUES ('e58ca1c7-178a-4012-8a71-8aa14eaf0655', '2015-12-17 08:42:00+00', NULL, NULL, '2015-12-17 08:40:00+00', 'SUCCESS', '1e697572-b896-484c-ae7f-0c8f63fcbc6c');
INSERT INTO public.propagationtaskexec VALUES ('c3290f8b-caf9-4a85-84fb-fb619b65cd49', '2015-12-17 08:42:00+00', NULL, NULL, '2015-12-17 08:40:00+00', 'SUCCESS', '025c956d-ea88-4bd7-9e44-2f35e0aa7055');
INSERT INTO public.propagationtaskexec VALUES ('d789462f-e395-424f-bd8e-0db44a93222f', '2015-12-17 08:42:00+00', NULL, NULL, '2015-12-17 08:40:00+00', 'SUCCESS', 'd6c2d6d3-6329-44c1-9187-f1469ead1cfa');


--
-- TOC entry 4835 (class 0 OID 16850)
-- Dependencies: 281
-- Data for Name: pulltask; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.pulltask VALUES ('c41b9b71-9bfa-4f90-89f2-84787def4c5c', 1, NULL, NULL, 'CSV (update matching; assign unmatching)', NULL, 'UPDATE', 1, 1, 1, 1, 'ASSIGN', 0, 'INCREMENTAL', 'PullJobDelegate', 'resource-csv', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL);
INSERT INTO public.pulltask VALUES ('83f7e85d-9774-43fe-adba-ccd856312994', 1, NULL, NULL, 'TestDB Task', NULL, 'UPDATE', 1, 0, 1, 1, 'PROVISION', 0, 'FULL_RECONCILIATION', 'PullJobDelegate', 'resource-testdb', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL);
INSERT INTO public.pulltask VALUES ('81d88f73-d474-4450-9031-605daa4e313f', 1, NULL, NULL, 'TestDB2 Task', NULL, 'UPDATE', 1, 0, 1, 1, 'PROVISION', 0, 'FULL_RECONCILIATION', 'PullJobDelegate', 'resource-testdb2', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL);
INSERT INTO public.pulltask VALUES ('7c2242f4-14af-4ab5-af31-cdae23783655', 1, NULL, NULL, 'TestDB Pull Task', NULL, 'UPDATE', 1, 1, 1, 1, 'PROVISION', 0, 'FULL_RECONCILIATION', 'PullJobDelegate', 'resource-db-pull', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL);
INSERT INTO public.pulltask VALUES ('1e419ca4-ea81-4493-a14f-28b90113686d', 1, NULL, NULL, 'LDAP Pull Task', NULL, 'UPDATE', 1, 1, 1, 0, 'PROVISION', 0, 'FULL_RECONCILIATION', 'PullJobDelegate', 'resource-ldap', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL);
INSERT INTO public.pulltask VALUES ('986867e2-993b-430e-8feb-aa9abb4c1dcd', 1, NULL, NULL, 'CSV Task (update matching; provision unmatching)', NULL, 'UPDATE', 1, 1, 1, 1, 'PROVISION', 0, 'INCREMENTAL', 'PullJobDelegate', 'resource-csv', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL);
INSERT INTO public.pulltask VALUES ('feae4e57-15ca-40d9-b973-8b9015efca49', 1, NULL, NULL, 'CSV (unlink matching; ignore unmatching)', NULL, 'UNLINK', 1, 1, 1, 1, 'IGNORE', 0, 'FULL_RECONCILIATION', 'PullJobDelegate', 'resource-csv', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL);
INSERT INTO public.pulltask VALUES ('55d5e74b-497e-4bc0-9156-73abef4b9adc', 1, NULL, NULL, 'CSV (ignore matching; assign unmatching)', NULL, 'IGNORE', 1, 1, 1, 1, 'ASSIGN', 0, 'FULL_RECONCILIATION', 'PullJobDelegate', 'resource-csv', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL);
INSERT INTO public.pulltask VALUES ('30cfd653-257b-495f-8665-281281dbcb3d', 1, NULL, NULL, 'Scripted SQL', NULL, 'UPDATE', 1, 0, 1, 0, 'PROVISION', 0, 'INCREMENTAL', 'PullJobDelegate', 'resource-db-scripted', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL);


--
-- TOC entry 4836 (class 0 OID 16859)
-- Dependencies: 282
-- Data for Name: pulltaskaction; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.pulltaskaction VALUES ('1e419ca4-ea81-4493-a14f-28b90113686d', 'LDAPMembershipPullActions');


--
-- TOC entry 4837 (class 0 OID 16864)
-- Dependencies: 283
-- Data for Name: pulltaskexec; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4838 (class 0 OID 16871)
-- Dependencies: 284
-- Data for Name: pushcorrelationruleentity; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.pushcorrelationruleentity VALUES ('24463935-32a0-4272-bc78-04d6d0adc69e', 'fb6530e5-892d-4f47-a46b-180c5b6c5c83', 'USER', 'TestPushCorrelationRule');


--
-- TOC entry 4839 (class 0 OID 16880)
-- Dependencies: 285
-- Data for Name: pushpolicy; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.pushpolicy VALUES ('fb6530e5-892d-4f47-a46b-180c5b6c5c83', 'a push policy', 'IGNORE');


--
-- TOC entry 4840 (class 0 OID 16885)
-- Dependencies: 286
-- Data for Name: pushtask; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.pushtask VALUES ('af558be4-9d2f-4359-bf85-a554e6e90be1', 1, NULL, NULL, 'Export on resource-testdb2.1', NULL, 'IGNORE', 1, 1, 1, 1, 'ASSIGN', '{"USER":"surname==Vivaldi","GROUP":"name==_NO_ONE_"}', 'PushJobDelegate', 'resource-testdb2', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.pushtask VALUES ('97f327b6-2eff-4d35-85e8-d581baaab855', 1, NULL, NULL, 'Export on resource-testdb2.2', NULL, 'IGNORE', 1, 1, 1, 1, 'PROVISION', '{"USER":"surname==Bellini","GROUP":"name==_NO_ONE_"}', 'PushJobDelegate', 'resource-testdb2', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.pushtask VALUES ('03aa2a04-4881-4573-9117-753f81b04865', 1, NULL, NULL, 'Export on resource-testdb2.3', NULL, 'IGNORE', 1, 1, 1, 1, 'UNLINK', '{"USER":"surname==Puccini","GROUP":"name==_NO_ONE_"}', 'PushJobDelegate', 'resource-testdb2', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.pushtask VALUES ('5e5f7c7e-9de7-4c6a-99f1-4df1af959807', 1, NULL, NULL, 'Export on resource-testdb2.4', NULL, 'IGNORE', 1, 1, 1, 1, 'IGNORE', '{"USER":"surname==Verdi","GROUP":"name==_NO_ONE_"}', 'PushJobDelegate', 'resource-testdb2', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.pushtask VALUES ('0bc11a19-6454-45c2-a4e3-ceef84e5d79b', 1, NULL, NULL, 'Export on resource-testdb2.5', NULL, 'UPDATE', 1, 1, 1, 1, 'ASSIGN', '{"USER":"username==_NO_ONE_","GROUP":"name==_NO_ONE_"}', 'PushJobDelegate', 'resource-testdb2', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.pushtask VALUES ('ec674143-480a-4816-98ad-b61fa090821e', 1, NULL, NULL, 'Export on resource-testdb2.6', NULL, 'DEPROVISION', 1, 1, 1, 1, 'IGNORE', '{"USER":"surname==Verdi","GROUP":"name==_NO_ONE_"}', 'PushJobDelegate', 'resource-testdb2', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.pushtask VALUES ('c46edc3a-a18b-4af2-b707-f4a415507496', 1, NULL, NULL, 'Export on resource-testdb2.7', NULL, 'UNASSIGN', 1, 1, 1, 1, 'IGNORE', '{"USER":"surname==Rossini","GROUP":"name==_NO_ONE_"}', 'PushJobDelegate', 'resource-testdb2', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.pushtask VALUES ('51318433-cce4-4f71-8f45-9534b6c9c819', 1, NULL, NULL, 'Export on resource-testdb2.8', NULL, 'LINK', 1, 1, 1, 1, 'IGNORE', '{"USER":"surname==Verdi","GROUP":"name==_NO_ONE_"}', 'PushJobDelegate', 'resource-testdb2', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.pushtask VALUES ('24b1be9c-7e3b-443a-86c9-798ebce5eaf2', 1, NULL, NULL, 'Export on resource-testdb2.9', NULL, 'UNLINK', 1, 1, 1, 1, 'IGNORE', '{"USER":"surname==Verdi","GROUP":"name==_NO_ONE_"}', 'PushJobDelegate', 'resource-testdb2', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.pushtask VALUES ('375c7b7f-9e3a-4833-88c9-b7787b0a69f2', 1, NULL, NULL, 'Export on resource-testdb2.10', NULL, 'UPDATE', 1, 1, 1, 1, 'IGNORE', '{"USER":"surname==Verdi","GROUP":"name==_NO_ONE_"}', 'PushJobDelegate', 'resource-testdb2', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.pushtask VALUES ('fd905ba5-9d56-4f51-83e2-859096a67b75', 1, NULL, NULL, 'Export on resource-ldap', NULL, 'UNLINK', 1, 1, 1, 1, 'ASSIGN', '{"USER":"username==_NO_ONE_","GROUP":"name==citizen"}', 'PushJobDelegate', 'resource-ldap', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');


--
-- TOC entry 4841 (class 0 OID 16894)
-- Dependencies: 287
-- Data for Name: pushtaskaction; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4842 (class 0 OID 16899)
-- Dependencies: 288
-- Data for Name: pushtaskexec; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4843 (class 0 OID 16906)
-- Dependencies: 289
-- Data for Name: realm; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.realm VALUES ('e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', '/', '/', NULL, NULL, NULL, NULL, NULL, NULL, '986d1236-3ac5-4a19-810c-5ab21d79cba1', NULL);
INSERT INTO public.realm VALUES ('722f3d84-9c2b-4525-8f6e-e4b82c55a36c', '/odd', 'odd', NULL, NULL, '06e2ed52-6966-44aa-a177-a0ca7434201f', NULL, NULL, 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL, NULL);
INSERT INTO public.realm VALUES ('c5b75db1-fce7-470f-b780-3b9934d82a9d', '/even', 'even', NULL, NULL, NULL, NULL, NULL, 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL, NULL);
INSERT INTO public.realm VALUES ('0679e069-7355-4b20-bd11-a5a0a5453c7c', '/even/two', 'two', NULL, NULL, '20ab5a8c-4b0c-432c-b957-f7fb9784d9f7', NULL, NULL, 'c5b75db1-fce7-470f-b780-3b9934d82a9d', 'ce93fcda-dc3a-4369-a7b0-a6108c261c85', NULL);


--
-- TOC entry 4845 (class 0 OID 16922)
-- Dependencies: 291
-- Data for Name: realm_anytypeclass; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4846 (class 0 OID 16925)
-- Dependencies: 292
-- Data for Name: realm_externalresource; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4844 (class 0 OID 16917)
-- Dependencies: 290
-- Data for Name: realmaction; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4847 (class 0 OID 16930)
-- Dependencies: 293
-- Data for Name: relationshiptype; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.relationshiptype VALUES ('inclusion', 'Models the act that an object is included in another', 'PRINTER', 'PRINTER');
INSERT INTO public.relationshiptype VALUES ('neighborhood', 'Models the act that an object is near another', 'USER', 'PRINTER');


--
-- TOC entry 4848 (class 0 OID 16937)
-- Dependencies: 294
-- Data for Name: remediation; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4849 (class 0 OID 16944)
-- Dependencies: 295
-- Data for Name: report; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.report VALUES ('0062ea9c-924d-4ecf-9961-4492a8cc6d1b', 1, NULL, 'pdf', 'application/pdf', 'test', 'SampleReportJobDelegate');


--
-- TOC entry 4850 (class 0 OID 16953)
-- Dependencies: 296
-- Data for Name: reportexec; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.reportexec VALUES ('c13f39c5-0d35-4bff-ba79-3cd5de940369', '2012-02-26 14:41:04+00', NULL, NULL, '2012-02-26 14:40:04+00', 'SUCCESS', NULL, '0062ea9c-924d-4ecf-9961-4492a8cc6d1b');


--
-- TOC entry 4851 (class 0 OID 16960)
-- Dependencies: 297
-- Data for Name: saml2idp4uiaction; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4852 (class 0 OID 16963)
-- Dependencies: 298
-- Data for Name: saml2idpentity; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4853 (class 0 OID 16970)
-- Dependencies: 299
-- Data for Name: saml2sp4uiidp; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4854 (class 0 OID 16981)
-- Dependencies: 300
-- Data for Name: saml2sp4uiusertemplate; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4855 (class 0 OID 16990)
-- Dependencies: 301
-- Data for Name: saml2spclientapp; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4856 (class 0 OID 17003)
-- Dependencies: 302
-- Data for Name: schedtask; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.schedtask VALUES ('e95555d2-1b09-42c8-b25b-f4c4ec597979', 1, '0 0 0 1 * ?', NULL, 'SampleJob Task', 'TestSampleJobDelegate');
INSERT INTO public.schedtask VALUES ('89de5014-e3f5-4462-84d8-d97575740baf', 1, '0 0/5 * * * ?', NULL, 'Access Token Cleanup Task', 'ExpiredAccessTokenCleanup');
INSERT INTO public.schedtask VALUES ('8ea0ea51-ce08-4fe3-a0c8-c281b31b5893', 1, '0 0/5 * * * ?', NULL, 'Expired Batch Operations Cleanup Task', 'ExpiredBatchCleanup');


--
-- TOC entry 4857 (class 0 OID 17012)
-- Dependencies: 303
-- Data for Name: schedtaskexec; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4858 (class 0 OID 17019)
-- Dependencies: 304
-- Data for Name: securityquestion; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.securityquestion VALUES ('887028ea-66fc-41e7-b397-620d7ea6dfbb', 'What''s your mother''s maiden name?');


--
-- TOC entry 4859 (class 0 OID 17026)
-- Dependencies: 305
-- Data for Name: sraroute; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sraroute VALUES ('ec7bada2-3dd6-460c-8441-65521d005ffa', 1, NULL, NULL, 0, 'basic1', NULL, '[{"cond":null,"factory":"METHOD","args":"GET"}]', NULL, 'PROTECTED', 'http://httpbin.org:80');


--
-- TOC entry 4860 (class 0 OID 17035)
-- Dependencies: 306
-- Data for Name: syncopebatch; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4861 (class 0 OID 17042)
-- Dependencies: 307
-- Data for Name: syncopedomain; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.syncopedomain VALUES ('Two', '{"_class":"org.apache.syncope.common.keymaster.client.api.model.JPADomain","key":"Two","adminPassword":"2AA60A8FF7FCD473D321E0146AFD9E26DF395147","adminCipherAlgorithm":"SHA","content":"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!--\nLicensed to the Apache Software Foundation (ASF) under one\nor more contributor license agreements.  See the NOTICE file\ndistributed with this work for additional information\nregarding copyright ownership.  The ASF licenses this file\nto you under the Apache License, Version 2.0 (the\n\"License\"); you may not use this file except in compliance\nwith the License.  You may obtain a copy of the License at\n\n  http://www.apache.org/licenses/LICENSE-2.0\n\nUnless required by applicable law or agreed to in writing,\nsoftware distributed under the License is distributed on an\n\"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\nKIND, either express or implied.  See the License for the\nspecific language governing permissions and limitations\nunder the License.\n-->\n<dataset>\n  <Realm id=\"ea696a4f-e77a-4ef1-be67-8f8093bc8686\" name=\"/\" fullPath=\"/\"/>\n\n  <AnyType id=\"USER\" kind=\"USER\"/>\n  <AnyTypeClass id=\"BaseUser\"/>\n  <AnyType_AnyTypeClass anyType_id=\"USER\" anyTypeClass_id=\"BaseUser\"/>\n\n  <AnyType id=\"GROUP\" kind=\"GROUP\"/>\n        \n  <Implementation id=\"EmailAddressValidator\" type=\"ATTR_VALUE_VALIDATOR\" engine=\"JAVA\"\n                  body=\"org.apache.syncope.core.persistence.common.attrvalue.EmailAddressValidator\"/>\n  <SyncopeSchema id=\"email\"/>\n  <PlainSchema id=\"email\" type=\"String\" anyTypeClass_id=\"BaseUser\"\n               mandatoryCondition=\"false\" multivalue=\"0\" uniqueConstraint=\"0\" readonly=\"0\"\n               validator_id=\"EmailAddressValidator\"/>\n\n  <Implementation id=\"BinaryValidator\" type=\"ATTR_VALUE_VALIDATOR\" engine=\"JAVA\"\n                  body=\"org.apache.syncope.core.persistence.common.attrvalue.BinaryValidator\"/>\n\n  <Implementation id=\"MacroJobDelegate\" type=\"TASKJOB_DELEGATE\" engine=\"JAVA\"\n                  body=\"org.apache.syncope.core.provisioning.java.job.MacroJobDelegate\"/>\n\n  <Implementation id=\"PullJobDelegate\" type=\"TASKJOB_DELEGATE\" engine=\"JAVA\"\n                  body=\"org.apache.syncope.core.provisioning.java.pushpull.PullJobDelegate\"/>\n  <Implementation id=\"PushJobDelegate\" type=\"TASKJOB_DELEGATE\" engine=\"JAVA\"\n                  body=\"org.apache.syncope.core.provisioning.java.pushpull.PushJobDelegate\"/>\n  <Implementation id=\"GroupMemberProvisionTaskJobDelegate\" type=\"TASKJOB_DELEGATE\" engine=\"JAVA\"\n                  body=\"org.apache.syncope.core.provisioning.java.job.GroupMemberProvisionTaskJobDelegate\"/>\n\n  <Implementation id=\"ExpiredAccessTokenCleanup\" type=\"TASKJOB_DELEGATE\" engine=\"JAVA\"\n                  body=\"org.apache.syncope.core.provisioning.java.job.ExpiredAccessTokenCleanup\"/>\n  <SchedTask id=\"89de5014-e3f5-4462-84d8-d97575740baf\" name=\"Access Token Cleanup Task\"  active=\"1\"\n             jobDelegate_id=\"ExpiredAccessTokenCleanup\" cronExpression=\"0 0/5 * * * ?\"/>\n  <Implementation id=\"ExpiredBatchCleanup\" type=\"TASKJOB_DELEGATE\" engine=\"JAVA\"\n                  body=\"org.apache.syncope.core.provisioning.java.job.ExpiredBatchCleanup\"/>\n  <SchedTask id=\"8ea0ea51-ce08-4fe3-a0c8-c281b31b5893\" name=\"Expired Batch Operations Cleanup Task\"  active=\"1\"\n             jobDelegate_id=\"ExpiredBatchCleanup\" cronExpression=\"0 0/5 * * * ?\"/>\n\n  <!-- Password reset notifications -->\n  <MailTemplate id=\"requestPasswordReset\"\n                textTemplate=\"Hi,\na password reset was requested for ${user.getUsername()}.\n\nIn order to complete this request, you need to visit this link:\n\nhttp://localhost:9080/syncope-enduser/confirmpasswordreset?token=${input.get(0).replaceAll('' '', ''%20'')}\n\nIf you did not request this reset, just ignore the present e-mail.\n\nBest regards.\"\n                htmlTemplate=\"&lt;html&gt;\n&lt;body&gt;\n&lt;p&gt;Hi,\na password reset was requested for ${user.getUsername()}.&lt;/p&gt;\n\n&lt;p&gt;In order to complete this request, you need to visit this \n&lt;a href=&quot;http://localhost:9080/syncope-enduser/confirmpasswordreset?token=${input.get(0).replaceAll('' '', ''%20'')}&quot;&gt;link&lt;/a&gt;&lt;/p&gt;.\n\n&lt;p&gt;If you did not request this reset, just ignore the present e-mail.&lt;/p&gt;\n\n&lt;p&gt;Best regards.&lt;/p&gt;\n&lt;/body&gt;\n&lt;/html&gt;\"/>\n  <MailTemplate id=\"confirmPasswordReset\"\n                textTemplate=\"Hi,\nwe are happy to inform you that the password request was successfully executed for your account.\n\nBest regards.\"\n                htmlTemplate=\"&lt;html&gt;\n&lt;body&gt;\n&lt;p&gt;Hi,&lt;/br&gt;\nwe are happy to inform you that the password request was successfully executed for your account.&lt;/p&gt;\n\n&lt;p&gt;Best regards.&lt;/p&gt;\n&lt;/body&gt;\n&lt;/html&gt;\"/>\n\n  <Notification id=\"c74b4616-9c63-4350-b4bf-ae0077b1ae6a\" active=\"1\" recipientAttrName=\"email\" selfAsRecipient=\"1\" \n                sender=\"admin@syncope.apache.org\" subject=\"Password Reset request\" template_id=\"requestPasswordReset\" \n                traceLevel=\"FAILURES\" events=''[\"[CUSTOM]:[]:[]:[requestPasswordReset]:[SUCCESS]\"]''/> \n  <AnyAbout id=\"0d4e37a1-a4f4-4865-afcb-4be01da3da53\" anyType_id=\"USER\" notification_id=\"c74b4616-9c63-4350-b4bf-ae0077b1ae6a\" anyType_filter=\"token!=$null\"/>\n  \n  <Notification id=\"71769807-7f74-4dc3-ba61-e4a7a00eb8ad\" active=\"1\" recipientAttrName=\"email\" selfAsRecipient=\"1\" \n                sender=\"admin@syncope.apache.org\" subject=\"Password Reset successful\" template_id=\"confirmPasswordReset\" \n                traceLevel=\"FAILURES\" events=''[\"[CUSTOM]:[]:[]:[confirmPasswordReset]:[SUCCESS]\"]''/> \n\n  <ConnInstance id=\"b7ea96c3-c633-488b-98a0-b52ac35850f7\" bundleName=\"net.tirasa.connid.bundles.ldap\" displayName=\"LDAP\"\n                adminRealm_id=\"ea696a4f-e77a-4ef1-be67-8f8093bc8686\"\n                location=\"${syncope.connid.location}\"\n                connectorName=\"net.tirasa.connid.bundles.ldap.LdapConnector\"\n                version=\"1.5.10\" \n                jsonConf=''[{\"schema\":{\"name\":\"synchronizePasswords\",\"displayName\":\"Enable Password Synchronization\",\"helpMessage\":\"If true, the connector will synchronize passwords. The Password Capture Plugin needs to be installed for password synchronization to work.\",\"type\":\"boolean\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"false\"]},{\"schema\":{\"name\":\"maintainLdapGroupMembership\",\"displayName\":\"Maintain LDAP Group Membership\",\"helpMessage\":\"When enabled and a user is renamed or deleted, update any LDAP groups to which the user belongs to reflect the new name. Otherwise, the LDAP resource must maintain referential integrity with respect to group membership.\",\"type\":\"boolean\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"true\"]},{\"schema\":{\"name\":\"host\",\"displayName\":\"Host\",\"helpMessage\":\"The name or IP address of the host where the LDAP server is running.\",\"type\":\"java.lang.String\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"localhost\"]},{\"schema\":{\"name\":\"passwordHashAlgorithm\",\"displayName\":\"Password Hash Algorithm\",\"helpMessage\":\"Indicates the algorithm that the Identity system should use to hash the password. Currently supported values are SSHA, SHA, SSHA1, and SHA1. A blank value indicates that the system will not hash passwords. This will cause cleartext passwords to be stored in LDAP unless the LDAP server performs the hash (Netscape Directory Server and iPlanet Directory Server do).\",\"type\":\"java.lang.String\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"SHA\"]},{\"schema\":{\"name\":\"port\",\"displayName\":\"TCP Port\",\"helpMessage\":\"TCP/IP port number used to communicate with the LDAP server.\",\"type\":\"int\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[1389]},{\"schema\":{\"name\":\"vlvSortAttribute\",\"displayName\":\"VLV Sort Attribute\",\"helpMessage\":\"Specify the sort attribute to use for VLV indexes on the resource.\",\"type\":\"java.lang.String\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[]},{\"schema\":{\"name\":\"statusManagementClass\",\"displayName\":\"Status management class \",\"helpMessage\":\"Class to be used to manage enabled/disabled status. If no class is specified then identity status management wont be possible.\",\"type\":\"java.lang.String\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"net.tirasa.connid.bundles.ldap.commons.AttributeStatusManagement\"]},{\"schema\":{\"name\":\"accountObjectClasses\",\"displayName\":\"Account Object Classes\",\"helpMessage\":\"The object class or classes that will be used when creating new user objects in the LDAP tree. When entering more than one object class, each entry should be on its own line; do not use commas or semi-colons to separate multiple object classes. Some object classes may require that you specify all object classes in the class hierarchy.\",\"type\":\"[Ljava.lang.String;\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"inetOrgPerson\"]},{\"schema\":{\"name\":\"accountUserNameAttributes\",\"displayName\":\"Account User Name Attributes\",\"helpMessage\":\"Attribute or attributes which holds the account user name. They will be used when authenticating to find the LDAP entry for the user name to authenticate.\",\"type\":\"[Ljava.lang.String;\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"uid\"]},{\"schema\":{\"name\":\"baseContextsToSynchronize\",\"displayName\":\"Base Contexts to Synchronize\",\"helpMessage\":\"One or more starting points in the LDAP tree that will be used to determine if a change should be synchronized. The base contexts attribute will be used to synchronize a change if this property is not set.\",\"type\":\"[Ljava.lang.String;\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"ou=people,o=isp\",\"ou=groups,o=isp\"]},{\"schema\":{\"name\":\"accountSynchronizationFilter\",\"displayName\":\"LDAP Filter for Accounts to Synchronize\",\"helpMessage\":\"An optional LDAP filter for the objects to synchronize. Because the change log is for all objects, this filter updates only objects that match the specified filter. If you specify a filter, an object will be synchronized only if it matches the filter and includes a synchronized object class.\",\"type\":\"java.lang.String\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[]},{\"schema\":{\"name\":\"removeLogEntryObjectClassFromFilter\",\"displayName\":\"Remove Log Entry Object Class from Filter\",\"helpMessage\":\"If this property is set (the default), the filter used to fetch change log entries does not contain the \\\"changeLogEntry\\\" object class, expecting that there are no entries of other object types in the change log.\",\"type\":\"boolean\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"false\"]},{\"schema\":{\"name\":\"passwordDecryptionKey\",\"displayName\":\"Password Decryption Key\",\"helpMessage\":\"The key to decrypt passwords with when performing password synchronization.\",\"type\":\"org.identityconnectors.common.security.GuardedByteArray\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[]},{\"schema\":{\"name\":\"readSchema\",\"displayName\":\"Read Schema\",\"helpMessage\":\"If true, the connector will read the schema from the server. If false, the connector will provide a default schema based on the object classes in the configuration. This property must be true in order to use extended object classes.\",\"type\":\"boolean\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"false\"]},{\"schema\":{\"name\":\"ssl\",\"displayName\":\"SSL\",\"helpMessage\":\"Select the check box to connect to the LDAP server using SSL.\",\"type\":\"boolean\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"false\"]},{\"schema\":{\"name\":\"passwordAttributeToSynchronize\",\"displayName\":\"Password Attribute to Synchronize\",\"helpMessage\":\"The name of the password attribute to synchronize when performing password synchronization.\",\"type\":\"java.lang.String\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[]},{\"schema\":{\"name\":\"accountSearchFilter\",\"displayName\":\"LDAP Filter for Retrieving Accounts\",\"helpMessage\":\"An optional LDAP filter to control which accounts are returned from the LDAP resource. If no filter is specified, only accounts that include all specified object classes are returned.\",\"type\":\"java.lang.String\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"uid=*\"]},{\"schema\":{\"name\":\"passwordDecryptionInitializationVector\",\"displayName\":\"Password Decryption Initialization Vector\",\"helpMessage\":\"The initialization vector to decrypt passwords with when performing password synchronization.\",\"type\":\"org.identityconnectors.common.security.GuardedByteArray\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[]},{\"schema\":{\"name\":\"groupMemberAttribute\",\"displayName\":\"Group Member Attribute\",\"helpMessage\":\"The name of the group attribute that will be updated with the distinguished name of the user when the user is added to the group.\",\"type\":\"java.lang.String\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[]},{\"schema\":{\"name\":\"failover\",\"displayName\":\"Failover Servers\",\"helpMessage\":\"List all servers that should be used for failover in case the preferred server fails. If the preferred server fails, JNDI will connect to the next available server in the list. List all servers in the form of \\\"ldap://ldap.example.com:389/\\\", which follows the standard LDAP v3 URLs described in RFC 2255. Only the host and port parts of the URL are relevant in this setting.\",\"type\":\"[Ljava.lang.String;\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[]},{\"schema\":{\"name\":\"modifiersNamesToFilterOut\",\"displayName\":\"Filter Out Changes By\",\"helpMessage\":\"The names (DNs) of directory administrators to filter from the changes. Changes with the attribute \\\"modifiersName\\\" that match entries in this list will be filtered out. The standard value is the administrator name used by this adapter, to prevent loops. Entries should be of the format \\\"cn=Directory Manager\\\".\",\"type\":\"[Ljava.lang.String;\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[]},{\"schema\":{\"name\":\"groupNameAttributes\",\"displayName\":\"Group Name Attributes\",\"helpMessage\":\"Attribute or attributes which holds the group name.\",\"type\":\"[Ljava.lang.String;\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"cn\"]},{\"schema\":{\"name\":\"uidAttribute\",\"displayName\":\"Uid Attribute\",\"helpMessage\":\"The name of the LDAP attribute which is mapped to the Uid attribute.\",\"type\":\"java.lang.String\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"cn\"]},{\"schema\":{\"name\":\"respectResourcePasswordPolicyChangeAfterReset\",\"displayName\":\"Respect Resource Password Policy Change-After-Reset\",\"helpMessage\":\"When this resource is specified in a Login Module (i.e., this resource is a pass-through authentication target) and the resource password policy is configured for change-after-reset, a user whose resource account password has been administratively reset will be required to change that password after successfully authenticating.\",\"type\":\"boolean\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"false\"]},{\"schema\":{\"name\":\"filterWithOrInsteadOfAnd\",\"displayName\":\"Filter with Or Instead of And\",\"helpMessage\":\"Normally the the filter used to fetch change log entries is an and-based filter retrieving an interval of change entries. If this property is set, the filter will or together the required change numbers instead.\",\"type\":\"boolean\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"false\"]},{\"schema\":{\"name\":\"principal\",\"displayName\":\"Principal\",\"helpMessage\":\"The distinguished name with which to authenticate to the LDAP server.\",\"type\":\"java.lang.String\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"uid=admin,ou=system\"]},{\"schema\":{\"name\":\"changeLogBlockSize\",\"displayName\":\"Change Log Block Size\",\"helpMessage\":\"The number of change log entries to fetch per query.\",\"type\":\"int\",\"required\":true,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[100]},{\"schema\":{\"name\":\"baseContexts\",\"displayName\":\"Base Contexts\",\"helpMessage\":\"One or more starting points in the LDAP tree that will be used when searching the tree. Searches are performed when discovering users from the LDAP server or when looking for the groups of which a user is a member.\",\"type\":\"[Ljava.lang.String;\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"ou=people,o=isp\",\"ou=groups,o=isp\"]},{\"schema\":{\"name\":\"passwordAttribute\",\"displayName\":\"Password Attribute\",\"helpMessage\":\"The name of the LDAP attribute which holds the password. When changing an user password, the new password is set to this attribute.\",\"type\":\"java.lang.String\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"userpassword\"]},{\"schema\":{\"name\":\"changeNumberAttribute\",\"displayName\":\"Change Number Attribute\",\"helpMessage\":\"The name of the change number attribute in the change log entry.\",\"type\":\"java.lang.String\",\"required\":true,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"changeNumber\"]},{\"schema\":{\"name\":\"objectClassesToSynchronize\",\"displayName\":\"Object Classes to Synchronize\",\"helpMessage\":\"The object classes to synchronize. The change log is for all objects; this filters updates to just the listed object classes. You should not list the superclasses of an object class unless you intend to synchronize objects with any of the superclass values. For example, if only \\\"inetOrgPerson\\\" objects should be synchronized, but the superclasses of \\\"inetOrgPerson\\\" (\\\"person\\\", \\\"organizationalperson\\\" and \\\"top\\\") should be filtered out, then list only \\\"inetOrgPerson\\\" here. All objects in LDAP are subclassed from \\\"top\\\". For this reason, you should never list \\\"top\\\", otherwise no object would be filtered.\",\"type\":\"[Ljava.lang.String;\",\"required\":true,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"inetOrgPerson\",\"groupOfUniqueNames\"]},{\"schema\":{\"name\":\"credentials\",\"displayName\":\"Password\",\"helpMessage\":\"Password for the principal.\",\"type\":\"org.identityconnectors.common.security.GuardedString\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"secret\"]},{\"schema\":{\"name\":\"attributesToSynchronize\",\"displayName\":\"Attributes to Synchronize\",\"helpMessage\":\"The names of the attributes to synchronize. This ignores updates from the change log if they do not update any of the named attributes. For example, if only \\\"department\\\" is listed, then only changes that affect \\\"department\\\" will be processed. All other updates are ignored. If blank (the default), then all changes are processed.\",\"type\":\"[Ljava.lang.String;\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[]},{\"schema\":{\"name\":\"maintainPosixGroupMembership\",\"displayName\":\"Maintain POSIX Group Membership\",\"helpMessage\":\"When enabled and a user is renamed or deleted, update any POSIX groups to which the user belongs to reflect the new name. Otherwise, the LDAP resource must maintain referential integrity with respect to group membership.\",\"type\":\"boolean\",\"required\":false,\"order\":0,\"confidential\":false,\"defaultValues\":null},\"overridable\":false,\"values\":[\"truemaintainLdapGroupMembership\"]}]''\n                capabilities=''[\"CREATE\",\"UPDATE\",\"DELETE\",\"SEARCH\"]''/>\n\n  <SyncopeRole id=\"GROUP_OWNER\" entitlements=''[\"USER_SEARCH\",\"USER_READ\",\"USER_CREATE\",\"USER_UPDATE\",\"USER_DELETE\",\"ANYTYPECLASS_READ\",\"ANYTYPE_LIST\",\"ANYTYPECLASS_LIST\",\"RELATIONSHIPTYPE_LIST\",\"ANYTYPE_READ\",\"REALM_SEARCH\",\"GROUP_SEARCH\",\"GROUP_READ\",\"GROUP_UPDATE\",\"GROUP_DELETE\"]''/>\n</dataset>\n","keymasterConfParams":"{\n  \"password.cipher.algorithm\": \"SHA1\",\n  \"token.length\": 256,\n  \"token.expireTime\": 60,\n  \"selfRegistration.allowed\": true,\n  \"passwordReset.allowed\": true,\n  \"passwordReset.securityQuestion\": true,\n  \"authentication.attributes\": [\"username\"],\n  \"authentication.statuses\": [\"created\", \"active\"],\n  \"log.lastlogindate\": true,\n  \"return.password.value\": false,\n  \"jwt.lifetime.minutes\": 120,\n  \"connector.conf.history.size\": 10,\n  \"resource.conf.history.size\": 10\n}","deployed":true,"jdbcDriver":"org.postgresql.Driver","jdbcURL":"jdbc:postgresql://localhost:5432/syncopetwo?stringtype=unspecified","dbSchema":null,"dbUsername":"syncopetwo","dbPassword":"syncopetwo","transactionIsolation":"TRANSACTION_READ_COMMITTED","poolMaxActive":20,"poolMinIdle":5,"orm":"META-INF/spring-orm.xml","databasePlatform":"org.apache.openjpa.jdbc.sql.PostgresDictionary"}');


--
-- TOC entry 4862 (class 0 OID 17049)
-- Dependencies: 308
-- Data for Name: syncopegroup; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.syncopegroup VALUES ('37d15e4c-cdc1-460b-a591-8505c8133806', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'root', '[{"schema": "icon", "values": [{"stringValue": "niceIcon"}]}, {"schema": "show", "values": [{"booleanValue": true}]}, {"schema": "rderived_sx", "values": [{"stringValue": "sx"}]}, {"schema": "rderived_dx", "values": [{"stringValue": "dx"}]}]', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL, NULL);
INSERT INTO public.syncopegroup VALUES ('b1f7c12d-ec83-441f-a50e-1691daaedf3b', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'child', '[{"schema": "icon", "values": [{"stringValue": "badIcon"}]}]', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL, NULL);
INSERT INTO public.syncopegroup VALUES ('29f96485-729e-4d31-88a1-6fc60e4677f3', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'citizen', NULL, 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL, NULL);
INSERT INTO public.syncopegroup VALUES ('8fb2d51e-c605-4e80-a72b-13ffecf1aa9a', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'employee', '[{"schema": "icon", "values": [{"stringValue": "icon4"}]}]', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL, NULL);
INSERT INTO public.syncopegroup VALUES ('a3c1a693-a6be-483f-a2b3-5cfec146f4bf', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'secretary', NULL, 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL, NULL);
INSERT INTO public.syncopegroup VALUES ('ebf97068-aa4b-4a85-9f01-680e8c4cf227', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'director', '[{"schema": "icon", "values": [{"stringValue": "icon6"}]}]', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL, '823074dc-d280-436d-a7dd-07399fae48ec');
INSERT INTO public.syncopegroup VALUES ('bf825fe1-7320-4a54-bd64-143b5c18ab97', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'managingDirector', NULL, 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL, NULL);
INSERT INTO public.syncopegroup VALUES ('f779c0d4-633b-4be5-8f57-32eb478a3ca5', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'otherchild', NULL, 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL, NULL);
INSERT INTO public.syncopegroup VALUES ('0cbcabd2-4410-4b6b-8f05-a052b451d18f', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'groupForWorkflowApproval', NULL, 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL, NULL);
INSERT INTO public.syncopegroup VALUES ('b8d38784-57e7-4595-859a-076222644b55', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'managingConsultant', NULL, 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL, NULL);
INSERT INTO public.syncopegroup VALUES ('268fed79-f440-4390-9435-b273768eb5d6', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'groupForWorkflowOptIn', NULL, 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL, NULL);
INSERT INTO public.syncopegroup VALUES ('0626100b-a4ba-4e00-9971-86fad52a6216', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'aGroupForPropagation', '[{"schema": "title", "values": [{"stringValue": "r12"}]}]', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL, NULL);
INSERT INTO public.syncopegroup VALUES ('ba9ed509-b1f5-48ab-a334-c8530a6422dc', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'bGroupForPropagation', '[{"schema": "title", "values": [{"stringValue": "r13"}]}]', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL, NULL);
INSERT INTO public.syncopegroup VALUES ('ece66293-8f31-4a84-8e8d-23da36e70846', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'artDirector', NULL, 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL, NULL);
INSERT INTO public.syncopegroup VALUES ('034740a9-fa10-453b-af37-dc7897e98fb1', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'additional', NULL, 'c5b75db1-fce7-470f-b780-3b9934d82a9d', NULL, NULL);
INSERT INTO public.syncopegroup VALUES ('e7ff94e8-19c9-4f0a-b8b7-28327edbf6ed', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, 'fake', NULL, '722f3d84-9c2b-4525-8f6e-e4b82c55a36c', NULL, NULL);


--
-- TOC entry 4863 (class 0 OID 17058)
-- Dependencies: 309
-- Data for Name: syncopegroup_anytypeclass; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.syncopegroup_anytypeclass VALUES ('0626100b-a4ba-4e00-9971-86fad52a6216', 'csv');
INSERT INTO public.syncopegroup_anytypeclass VALUES ('ba9ed509-b1f5-48ab-a334-c8530a6422dc', 'csv');


--
-- TOC entry 4864 (class 0 OID 17063)
-- Dependencies: 310
-- Data for Name: syncopegroup_externalresource; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.syncopegroup_externalresource VALUES ('29f96485-729e-4d31-88a1-6fc60e4677f3', 'ws-target-resource-list-mappings-1');
INSERT INTO public.syncopegroup_externalresource VALUES ('f779c0d4-633b-4be5-8f57-32eb478a3ca5', 'ws-target-resource-2');
INSERT INTO public.syncopegroup_externalresource VALUES ('29f96485-729e-4d31-88a1-6fc60e4677f3', 'ws-target-resource-list-mappings-2');
INSERT INTO public.syncopegroup_externalresource VALUES ('bf825fe1-7320-4a54-bd64-143b5c18ab97', 'ws-target-resource-nopropagation');
INSERT INTO public.syncopegroup_externalresource VALUES ('b8d38784-57e7-4595-859a-076222644b55', 'ws-target-resource-nopropagation3');
INSERT INTO public.syncopegroup_externalresource VALUES ('0626100b-a4ba-4e00-9971-86fad52a6216', 'resource-csv');
INSERT INTO public.syncopegroup_externalresource VALUES ('ba9ed509-b1f5-48ab-a334-c8530a6422dc', 'resource-csv');


--
-- TOC entry 4865 (class 0 OID 17068)
-- Dependencies: 311
-- Data for Name: syncoperole; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.syncoperole VALUES ('User reviewer', NULL, NULL, '["USER_READ","USER_LIST","USER_SEARCH","ANYTYPE_LIST","ANYTYPE_READ","ANYTYPECLASS_LIST","ANYTYPECLASS_READ"]');
INSERT INTO public.syncoperole VALUES ('User manager', NULL, NULL, '["USER_READ","USER_LIST","USER_SEARCH","ANYTYPE_LIST","ANYTYPE_READ","ANYTYPECLASS_LIST","ANYTYPECLASS_READ","USER_REQUEST_FORM_LIST","USER_REQUEST_FORM_CLAIM","USER_REQUEST_FORM_SUBMIT"]');
INSERT INTO public.syncoperole VALUES ('Other', NULL, NULL, '["SCHEMA_READ","GROUP_READ","USER_REQUEST_FORM_CLAIM"]');
INSERT INTO public.syncoperole VALUES ('Search for realm evenTwo', NULL, NULL, '["USER_READ","USER_SEARCH"]');
INSERT INTO public.syncoperole VALUES ('Connector and Resource for realm evenTwo', NULL, NULL, '["CONNECTOR_READ","CONNECTOR_UPDATE","CONNECTOR_DELETE","CONNECTOR_LIST","RESOURCE_READ","RESOURCE_UPDATE","RESOURCE_DELETE","RESOURCE_LIST"]');
INSERT INTO public.syncoperole VALUES ('GROUP_OWNER', NULL, NULL, '["USER_SEARCH","USER_READ","USER_CREATE","USER_UPDATE","USER_DELETE","ANYTYPECLASS_READ","ANYTYPE_LIST","ANYTYPECLASS_LIST","RELATIONSHIPTYPE_LIST","ANYTYPE_READ","REALM_SEARCH","GROUP_SEARCH","GROUP_READ","GROUP_UPDATE","GROUP_DELETE"]');


--
-- TOC entry 4866 (class 0 OID 17075)
-- Dependencies: 312
-- Data for Name: syncoperole_dynrealm; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4867 (class 0 OID 17082)
-- Dependencies: 313
-- Data for Name: syncoperole_realm; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.syncoperole_realm VALUES ('User reviewer', '722f3d84-9c2b-4525-8f6e-e4b82c55a36c');
INSERT INTO public.syncoperole_realm VALUES ('User reviewer', 'c5b75db1-fce7-470f-b780-3b9934d82a9d');
INSERT INTO public.syncoperole_realm VALUES ('User manager', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28');
INSERT INTO public.syncoperole_realm VALUES ('Other', '722f3d84-9c2b-4525-8f6e-e4b82c55a36c');
INSERT INTO public.syncoperole_realm VALUES ('Search for realm evenTwo', '0679e069-7355-4b20-bd11-a5a0a5453c7c');
INSERT INTO public.syncoperole_realm VALUES ('Connector and Resource for realm evenTwo', '0679e069-7355-4b20-bd11-a5a0a5453c7c');


--
-- TOC entry 4868 (class 0 OID 17087)
-- Dependencies: 314
-- Data for Name: syncopeschema; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.syncopeschema VALUES ('fullname', NULL);
INSERT INTO public.syncopeschema VALUES ('userId', NULL);
INSERT INTO public.syncopeschema VALUES ('loginDate', NULL);
INSERT INTO public.syncopeschema VALUES ('firstname', '{"en":"Firstname","it":"Nome","pt_BR":"Nome"}');
INSERT INTO public.syncopeschema VALUES ('surname', '{"en":"Surname","it":"Cognome"}');
INSERT INTO public.syncopeschema VALUES ('ctype', NULL);
INSERT INTO public.syncopeschema VALUES ('email', NULL);
INSERT INTO public.syncopeschema VALUES ('activationDate', NULL);
INSERT INTO public.syncopeschema VALUES ('uselessReadonly', NULL);
INSERT INTO public.syncopeschema VALUES ('cool', NULL);
INSERT INTO public.syncopeschema VALUES ('gender', NULL);
INSERT INTO public.syncopeschema VALUES ('dd', NULL);
INSERT INTO public.syncopeschema VALUES ('aLong', NULL);
INSERT INTO public.syncopeschema VALUES ('makeItDouble', NULL);
INSERT INTO public.syncopeschema VALUES ('obscure', NULL);
INSERT INTO public.syncopeschema VALUES ('photo', NULL);
INSERT INTO public.syncopeschema VALUES ('csvuserid', NULL);
INSERT INTO public.syncopeschema VALUES ('cn', NULL);
INSERT INTO public.syncopeschema VALUES ('noschema', NULL);
INSERT INTO public.syncopeschema VALUES ('info', NULL);
INSERT INTO public.syncopeschema VALUES ('icon', NULL);
INSERT INTO public.syncopeschema VALUES ('show', NULL);
INSERT INTO public.syncopeschema VALUES ('rderived_sx', NULL);
INSERT INTO public.syncopeschema VALUES ('rderived_dx', NULL);
INSERT INTO public.syncopeschema VALUES ('title', NULL);
INSERT INTO public.syncopeschema VALUES ('originalName', NULL);
INSERT INTO public.syncopeschema VALUES ('rderiveddata', NULL);
INSERT INTO public.syncopeschema VALUES ('displayProperty', NULL);
INSERT INTO public.syncopeschema VALUES ('rderToBePropagated', NULL);
INSERT INTO public.syncopeschema VALUES ('rderivedschema', NULL);
INSERT INTO public.syncopeschema VALUES ('subscriptionDate', NULL);
INSERT INTO public.syncopeschema VALUES ('mderived_sx', NULL);
INSERT INTO public.syncopeschema VALUES ('mderived_dx', NULL);
INSERT INTO public.syncopeschema VALUES ('postalAddress', NULL);
INSERT INTO public.syncopeschema VALUES ('mderiveddata', NULL);
INSERT INTO public.syncopeschema VALUES ('mderToBePropagated', NULL);
INSERT INTO public.syncopeschema VALUES ('model', NULL);
INSERT INTO public.syncopeschema VALUES ('location', NULL);


--
-- TOC entry 4869 (class 0 OID 17094)
-- Dependencies: 315
-- Data for Name: syncopeuser; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.syncopeuser VALUES ('1417acbe-cbf6-4277-9372-e75e04f97000', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', 'active', NULL, 'SHA1', NULL, NULL, 0, '5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8', NULL, '[{"schema": "ctype", "values": [{"stringValue": "G"}]}, {"schema": "firstname", "values": [{"stringValue": "Gioacchino"}]}, {"schema": "surname", "values": [{"stringValue": "Rossini"}]}, {"schema": "loginDate", "values": [{"dateValue": "2009-05-26T00:00:00+02:00"}, {"dateValue": "2010-05-26T00:00:00+02:00"}]}, {"schema": "fullname", "uniqueValue": {"stringValue": "Gioacchino Rossini"}}, {"schema": "userId", "uniqueValue": {"stringValue": "rossini@apache.org"}}]', NULL, 0, NULL, NULL, 'rossini', 'c5b75db1-fce7-470f-b780-3b9934d82a9d', NULL);
INSERT INTO public.syncopeuser VALUES ('74cd8ece-715a-44a4-a736-e17b46c4e7e6', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', 'active', NULL, 'SHA1', NULL, NULL, 0, '5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8', NULL, '[{"schema": "firstname", "values": [{"stringValue": "Giuseppe"}]}, {"schema": "surname", "values": [{"stringValue": "Verdi"}]}, {"schema": "email", "values": [{"stringValue": "verdi@syncope.org"}]}, {"schema": "fullname", "uniqueValue": {"stringValue": "Giuseppe Verdi"}}, {"schema": "userId", "uniqueValue": {"stringValue": "verdi@apache.org"}}]', NULL, 0, NULL, NULL, 'verdi', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL);
INSERT INTO public.syncopeuser VALUES ('b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', 'active', NULL, 'SHA1', NULL, NULL, 0, '5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8', NULL, '[{"schema": "firstname", "values": [{"stringValue": "Antonio"}]}, {"schema": "surname", "values": [{"stringValue": "Vivaldi"}]}, {"schema": "email", "values": [{"stringValue": "vivaldi@syncope.org"}]}, {"schema": "ctype", "values": [{"stringValue": "F"}]}, {"schema": "fullname", "uniqueValue": {"stringValue": "Antonio Vivaldi"}}, {"schema": "userId", "uniqueValue": {"stringValue": "vivaldi@apache.org"}}]', NULL, 0, NULL, NULL, 'vivaldi', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL);
INSERT INTO public.syncopeuser VALUES ('c9b2dec2-00a7-4855-97c0-d854842b4b24', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', 'active', NULL, 'SHA1', NULL, '2016-03-03 14:21:22+00', 0, '5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8', NULL, '[{"schema": "firstname", "values": [{"stringValue": "Vincenzo"}]}, {"schema": "surname", "values": [{"stringValue": "Bellini"}]}, {"schema": "loginDate", "values": [{"dateValue": "2009-06-24T00:00:00+02:00"}]}, {"schema": "cool", "values": [{"booleanValue": true}]}, {"schema": "gender", "values": [{"stringValue": "M"}]}, {"schema": "fullname", "uniqueValue": {"stringValue": "Vincenzo Bellini"}}, {"schema": "userId", "uniqueValue": {"stringValue": "bellini@apache.org"}}]', NULL, 0, NULL, NULL, 'bellini', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL);
INSERT INTO public.syncopeuser VALUES ('823074dc-d280-436d-a7dd-07399fae48ec', NULL, '2010-10-20 10:00:00+00', 'admin', NULL, '2010-10-20 10:00:00+00', 'admin', 'active', NULL, 'SHA1', NULL, NULL, 0, '5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8', NULL, '[{"schema": "firstname", "values": [{"stringValue": "Giacomo"}]}, {"schema": "surname", "values": [{"stringValue": "Puccini"}]}, {"schema": "fullname", "uniqueValue": {"stringValue": "Giacomo Puccini"}}, {"schema": "userId", "uniqueValue": {"stringValue": "puccini@apache.org"}}]', NULL, 0, NULL, NULL, 'puccini', 'e4c28e7a-9dbf-4ee7-9441-93812a0d4a28', NULL);


--
-- TOC entry 4870 (class 0 OID 17103)
-- Dependencies: 316
-- Data for Name: syncopeuser_anytypeclass; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4871 (class 0 OID 17108)
-- Dependencies: 317
-- Data for Name: syncopeuser_externalresource; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.syncopeuser_externalresource VALUES ('1417acbe-cbf6-4277-9372-e75e04f97000', 'resource-testdb2');
INSERT INTO public.syncopeuser_externalresource VALUES ('b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee', 'ws-target-resource-delete');
INSERT INTO public.syncopeuser_externalresource VALUES ('b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee', 'ws-target-resource-2');
INSERT INTO public.syncopeuser_externalresource VALUES ('b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee', 'ws-target-resource-1');
INSERT INTO public.syncopeuser_externalresource VALUES ('823074dc-d280-436d-a7dd-07399fae48ec', 'resource-testdb2');


--
-- TOC entry 4872 (class 0 OID 17113)
-- Dependencies: 318
-- Data for Name: syncopeuser_syncoperole; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.syncopeuser_syncoperole VALUES ('1417acbe-cbf6-4277-9372-e75e04f97000', 'Other');
INSERT INTO public.syncopeuser_syncoperole VALUES ('c9b2dec2-00a7-4855-97c0-d854842b4b24', 'User reviewer');
INSERT INTO public.syncopeuser_syncoperole VALUES ('c9b2dec2-00a7-4855-97c0-d854842b4b24', 'User manager');
INSERT INTO public.syncopeuser_syncoperole VALUES ('823074dc-d280-436d-a7dd-07399fae48ec', 'Search for realm evenTwo');
INSERT INTO public.syncopeuser_syncoperole VALUES ('823074dc-d280-436d-a7dd-07399fae48ec', 'Connector and Resource for realm evenTwo');


--
-- TOC entry 4873 (class 0 OID 17118)
-- Dependencies: 319
-- Data for Name: ticketexpirationpolicy; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4874 (class 0 OID 17125)
-- Dependencies: 320
-- Data for Name: typeextension; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.typeextension VALUES ('88a71478-30aa-4ee0-8b2b-6cb32e7ba264', 'f779c0d4-633b-4be5-8f57-32eb478a3ca5', 'PRINTER');
INSERT INTO public.typeextension VALUES ('84c1490c-a1d9-4b91-859c-fafbb0113a85', '034740a9-fa10-453b-af37-dc7897e98fb1', 'USER');


--
-- TOC entry 4875 (class 0 OID 17132)
-- Dependencies: 321
-- Data for Name: typeextension_anytypeclass; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.typeextension_anytypeclass VALUES ('88a71478-30aa-4ee0-8b2b-6cb32e7ba264', 'other');
INSERT INTO public.typeextension_anytypeclass VALUES ('84c1490c-a1d9-4b91-859c-fafbb0113a85', 'csv');
INSERT INTO public.typeextension_anytypeclass VALUES ('84c1490c-a1d9-4b91-859c-fafbb0113a85', 'other');


--
-- TOC entry 4926 (class 0 OID 18696)
-- Dependencies: 372
-- Data for Name: udyngroupmembers; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4876 (class 0 OID 17137)
-- Dependencies: 322
-- Data for Name: udyngroupmembership; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4877 (class 0 OID 17142)
-- Dependencies: 323
-- Data for Name: umembership; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.umembership VALUES ('3d5e91f6-305e-45f9-ad30-4897d3d43bd9', '1417acbe-cbf6-4277-9372-e75e04f97000', '37d15e4c-cdc1-460b-a591-8505c8133806');
INSERT INTO public.umembership VALUES ('d53f7657-2b22-4e10-a2cd-c3379a4d1a31', '74cd8ece-715a-44a4-a736-e17b46c4e7e6', '37d15e4c-cdc1-460b-a591-8505c8133806');
INSERT INTO public.umembership VALUES ('8e42a132-55ae-4860-bebd-2ca00ba5e959', '74cd8ece-715a-44a4-a736-e17b46c4e7e6', 'b1f7c12d-ec83-441f-a50e-1691daaedf3b');
INSERT INTO public.umembership VALUES ('40e409a4-d870-4792-b820-30668f1269b9', 'c9b2dec2-00a7-4855-97c0-d854842b4b24', 'bf825fe1-7320-4a54-bd64-143b5c18ab97');
INSERT INTO public.umembership VALUES ('6d8a7dc0-d4bc-4b7e-b058-abcd3df28f28', '1417acbe-cbf6-4277-9372-e75e04f97000', 'f779c0d4-633b-4be5-8f57-32eb478a3ca5');
INSERT INTO public.umembership VALUES ('34f2d776-58b1-4640-8e64-e979b4242a18', '74cd8ece-715a-44a4-a736-e17b46c4e7e6', '29f96485-729e-4d31-88a1-6fc60e4677f3');
INSERT INTO public.umembership VALUES ('8cfb78fc-d0e7-4f08-a0ae-d7abf3223b6f', '823074dc-d280-436d-a7dd-07399fae48ec', 'ece66293-8f31-4a84-8e8d-23da36e70846');


--
-- TOC entry 4878 (class 0 OID 17147)
-- Dependencies: 324
-- Data for Name: urelationship; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.urelationship VALUES ('ca20ffca-1305-442f-be9a-3723a0cd88ca', 'c9b2dec2-00a7-4855-97c0-d854842b4b24', 'fc6dbc3a-6c07-4965-8781-921e7401a4a5', 'neighborhood');


--
-- TOC entry 4879 (class 0 OID 17154)
-- Dependencies: 325
-- Data for Name: waconfigentry; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 4934 (class 0 OID 0)
-- Dependencies: 351
-- Name: act_evt_log_log_nr__seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.act_evt_log_log_nr__seq', 1, false);


--
-- TOC entry 4935 (class 0 OID 0)
-- Dependencies: 342
-- Name: act_hi_tsk_log_id__seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.act_hi_tsk_log_id__seq', 1, false);


--
-- TOC entry 3830 (class 2606 OID 16396)
-- Name: accesspolicy accesspolicy_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accesspolicy
    ADD CONSTRAINT accesspolicy_pkey PRIMARY KEY (id);


--
-- TOC entry 3832 (class 2606 OID 16403)
-- Name: accesstoken accesstoken_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accesstoken
    ADD CONSTRAINT accesstoken_pkey PRIMARY KEY (id);


--
-- TOC entry 3836 (class 2606 OID 16410)
-- Name: accountpolicy accountpolicy_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accountpolicy
    ADD CONSTRAINT accountpolicy_pkey PRIMARY KEY (id);


--
-- TOC entry 4317 (class 2606 OID 18324)
-- Name: act_evt_log act_evt_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_evt_log
    ADD CONSTRAINT act_evt_log_pkey PRIMARY KEY (log_nr_);


--
-- TOC entry 4164 (class 2606 OID 18012)
-- Name: act_ge_bytearray act_ge_bytearray_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ge_bytearray
    ADD CONSTRAINT act_ge_bytearray_pkey PRIMARY KEY (id_);


--
-- TOC entry 4162 (class 2606 OID 18005)
-- Name: act_ge_property act_ge_property_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ge_property
    ADD CONSTRAINT act_ge_property_pkey PRIMARY KEY (name_);


--
-- TOC entry 4341 (class 2606 OID 18558)
-- Name: act_hi_actinst act_hi_actinst_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_hi_actinst
    ADD CONSTRAINT act_hi_actinst_pkey PRIMARY KEY (id_);


--
-- TOC entry 4356 (class 2606 OID 18579)
-- Name: act_hi_attachment act_hi_attachment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_hi_attachment
    ADD CONSTRAINT act_hi_attachment_pkey PRIMARY KEY (id_);


--
-- TOC entry 4354 (class 2606 OID 18572)
-- Name: act_hi_comment act_hi_comment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_hi_comment
    ADD CONSTRAINT act_hi_comment_pkey PRIMARY KEY (id_);


--
-- TOC entry 4347 (class 2606 OID 18565)
-- Name: act_hi_detail act_hi_detail_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_hi_detail
    ADD CONSTRAINT act_hi_detail_pkey PRIMARY KEY (id_);


--
-- TOC entry 4173 (class 2606 OID 18030)
-- Name: act_hi_entitylink act_hi_entitylink_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_hi_entitylink
    ADD CONSTRAINT act_hi_entitylink_pkey PRIMARY KEY (id_);


--
-- TOC entry 4189 (class 2606 OID 18053)
-- Name: act_hi_identitylink act_hi_identitylink_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_hi_identitylink
    ADD CONSTRAINT act_hi_identitylink_pkey PRIMARY KEY (id_);


--
-- TOC entry 4334 (class 2606 OID 18547)
-- Name: act_hi_procinst act_hi_procinst_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_hi_procinst
    ADD CONSTRAINT act_hi_procinst_pkey PRIMARY KEY (id_);


--
-- TOC entry 4336 (class 2606 OID 18549)
-- Name: act_hi_procinst act_hi_procinst_proc_inst_id__key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_hi_procinst
    ADD CONSTRAINT act_hi_procinst_proc_inst_id__key UNIQUE (proc_inst_id_);


--
-- TOC entry 4266 (class 2606 OID 18229)
-- Name: act_hi_taskinst act_hi_taskinst_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_hi_taskinst
    ADD CONSTRAINT act_hi_taskinst_pkey PRIMARY KEY (id_);


--
-- TOC entry 4272 (class 2606 OID 18239)
-- Name: act_hi_tsk_log act_hi_tsk_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_hi_tsk_log
    ADD CONSTRAINT act_hi_tsk_log_pkey PRIMARY KEY (id_);


--
-- TOC entry 4283 (class 2606 OID 18266)
-- Name: act_hi_varinst act_hi_varinst_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_hi_varinst
    ADD CONSTRAINT act_hi_varinst_pkey PRIMARY KEY (id_);


--
-- TOC entry 4360 (class 2606 OID 18609)
-- Name: act_id_bytearray act_id_bytearray_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_id_bytearray
    ADD CONSTRAINT act_id_bytearray_pkey PRIMARY KEY (id_);


--
-- TOC entry 4362 (class 2606 OID 18616)
-- Name: act_id_group act_id_group_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_id_group
    ADD CONSTRAINT act_id_group_pkey PRIMARY KEY (id_);


--
-- TOC entry 4370 (class 2606 OID 18636)
-- Name: act_id_info act_id_info_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_id_info
    ADD CONSTRAINT act_id_info_pkey PRIMARY KEY (id_);


--
-- TOC entry 4364 (class 2606 OID 18621)
-- Name: act_id_membership act_id_membership_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_id_membership
    ADD CONSTRAINT act_id_membership_pkey PRIMARY KEY (user_id_, group_id_);


--
-- TOC entry 4378 (class 2606 OID 18655)
-- Name: act_id_priv_mapping act_id_priv_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_id_priv_mapping
    ADD CONSTRAINT act_id_priv_mapping_pkey PRIMARY KEY (id_);


--
-- TOC entry 4374 (class 2606 OID 18648)
-- Name: act_id_priv act_id_priv_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_id_priv
    ADD CONSTRAINT act_id_priv_pkey PRIMARY KEY (id_);


--
-- TOC entry 4358 (class 2606 OID 18602)
-- Name: act_id_property act_id_property_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_id_property
    ADD CONSTRAINT act_id_property_pkey PRIMARY KEY (name_);


--
-- TOC entry 4372 (class 2606 OID 18643)
-- Name: act_id_token act_id_token_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_id_token
    ADD CONSTRAINT act_id_token_pkey PRIMARY KEY (id_);


--
-- TOC entry 4368 (class 2606 OID 18629)
-- Name: act_id_user act_id_user_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_id_user
    ADD CONSTRAINT act_id_user_pkey PRIMARY KEY (id_);


--
-- TOC entry 4321 (class 2606 OID 18329)
-- Name: act_procdef_info act_procdef_info_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_procdef_info
    ADD CONSTRAINT act_procdef_info_pkey PRIMARY KEY (id_);


--
-- TOC entry 4297 (class 2606 OID 18289)
-- Name: act_re_deployment act_re_deployment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_re_deployment
    ADD CONSTRAINT act_re_deployment_pkey PRIMARY KEY (id_);


--
-- TOC entry 4302 (class 2606 OID 18297)
-- Name: act_re_model act_re_model_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_re_model
    ADD CONSTRAINT act_re_model_pkey PRIMARY KEY (id_);


--
-- TOC entry 4313 (class 2606 OID 18314)
-- Name: act_re_procdef act_re_procdef_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_re_procdef
    ADD CONSTRAINT act_re_procdef_pkey PRIMARY KEY (id_);


--
-- TOC entry 4332 (class 2606 OID 18338)
-- Name: act_ru_actinst act_ru_actinst_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_actinst
    ADD CONSTRAINT act_ru_actinst_pkey PRIMARY KEY (id_);


--
-- TOC entry 4240 (class 2606 OID 18089)
-- Name: act_ru_deadletter_job act_ru_deadletter_job_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_deadletter_job
    ADD CONSTRAINT act_ru_deadletter_job_pkey PRIMARY KEY (id_);


--
-- TOC entry 4171 (class 2606 OID 18019)
-- Name: act_ru_entitylink act_ru_entitylink_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_entitylink
    ADD CONSTRAINT act_ru_entitylink_pkey PRIMARY KEY (id_);


--
-- TOC entry 4295 (class 2606 OID 18277)
-- Name: act_ru_event_subscr act_ru_event_subscr_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_event_subscr
    ADD CONSTRAINT act_ru_event_subscr_pkey PRIMARY KEY (id_);


--
-- TOC entry 4311 (class 2606 OID 18305)
-- Name: act_ru_execution act_ru_execution_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_execution
    ADD CONSTRAINT act_ru_execution_pkey PRIMARY KEY (id_);


--
-- TOC entry 4250 (class 2606 OID 18105)
-- Name: act_ru_external_job act_ru_external_job_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_external_job
    ADD CONSTRAINT act_ru_external_job_pkey PRIMARY KEY (id_);


--
-- TOC entry 4242 (class 2606 OID 18097)
-- Name: act_ru_history_job act_ru_history_job_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_history_job
    ADD CONSTRAINT act_ru_history_job_pkey PRIMARY KEY (id_);


--
-- TOC entry 4187 (class 2606 OID 18041)
-- Name: act_ru_identitylink act_ru_identitylink_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_identitylink
    ADD CONSTRAINT act_ru_identitylink_pkey PRIMARY KEY (id_);


--
-- TOC entry 4206 (class 2606 OID 18065)
-- Name: act_ru_job act_ru_job_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_job
    ADD CONSTRAINT act_ru_job_pkey PRIMARY KEY (id_);


--
-- TOC entry 4229 (class 2606 OID 18081)
-- Name: act_ru_suspended_job act_ru_suspended_job_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_suspended_job
    ADD CONSTRAINT act_ru_suspended_job_pkey PRIMARY KEY (id_);


--
-- TOC entry 4264 (class 2606 OID 18216)
-- Name: act_ru_task act_ru_task_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_task
    ADD CONSTRAINT act_ru_task_pkey PRIMARY KEY (id_);


--
-- TOC entry 4218 (class 2606 OID 18073)
-- Name: act_ru_timer_job act_ru_timer_job_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_timer_job
    ADD CONSTRAINT act_ru_timer_job_pkey PRIMARY KEY (id_);


--
-- TOC entry 4281 (class 2606 OID 18250)
-- Name: act_ru_variable act_ru_variable_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_variable
    ADD CONSTRAINT act_ru_variable_pkey PRIMARY KEY (id_);


--
-- TOC entry 4323 (class 2606 OID 18538)
-- Name: act_procdef_info act_uniq_info_procdef; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_procdef_info
    ADD CONSTRAINT act_uniq_info_procdef UNIQUE (proc_def_id_);


--
-- TOC entry 4376 (class 2606 OID 18677)
-- Name: act_id_priv act_uniq_priv_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_id_priv
    ADD CONSTRAINT act_uniq_priv_name UNIQUE (name_);


--
-- TOC entry 4315 (class 2606 OID 18357)
-- Name: act_re_procdef act_uniq_procdef; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_re_procdef
    ADD CONSTRAINT act_uniq_procdef UNIQUE (key_, version_, derived_version_, tenant_id_);


--
-- TOC entry 4384 (class 2606 OID 18685)
-- Name: adyngroupmembers adyngroupmembers_anytype_id_any_id_group_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.adyngroupmembers
    ADD CONSTRAINT adyngroupmembers_anytype_id_any_id_group_id_key UNIQUE (anytype_id, any_id, group_id);


--
-- TOC entry 3840 (class 2606 OID 16422)
-- Name: adyngroupmembership adyngroupmembership_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.adyngroupmembership
    ADD CONSTRAINT adyngroupmembership_pkey PRIMARY KEY (id);


--
-- TOC entry 3844 (class 2606 OID 16427)
-- Name: amembership amembership_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.amembership
    ADD CONSTRAINT amembership_pkey PRIMARY KEY (id);


--
-- TOC entry 3846 (class 2606 OID 16434)
-- Name: anyabout anyabout_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anyabout
    ADD CONSTRAINT anyabout_pkey PRIMARY KEY (id);


--
-- TOC entry 3852 (class 2606 OID 16443)
-- Name: anyobject anyobject_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anyobject
    ADD CONSTRAINT anyobject_pkey PRIMARY KEY (id);


--
-- TOC entry 3862 (class 2606 OID 16462)
-- Name: anytemplatelivesynctask anytemplatelivesynctask_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anytemplatelivesynctask
    ADD CONSTRAINT anytemplatelivesynctask_pkey PRIMARY KEY (id);


--
-- TOC entry 3866 (class 2606 OID 16471)
-- Name: anytemplatepulltask anytemplatepulltask_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anytemplatepulltask
    ADD CONSTRAINT anytemplatepulltask_pkey PRIMARY KEY (id);


--
-- TOC entry 3871 (class 2606 OID 16480)
-- Name: anytemplaterealm anytemplaterealm_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anytemplaterealm
    ADD CONSTRAINT anytemplaterealm_pkey PRIMARY KEY (id);


--
-- TOC entry 3875 (class 2606 OID 16487)
-- Name: anytype anytype_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anytype
    ADD CONSTRAINT anytype_pkey PRIMARY KEY (id);


--
-- TOC entry 3877 (class 2606 OID 16492)
-- Name: anytypeclass anytypeclass_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anytypeclass
    ADD CONSTRAINT anytypeclass_pkey PRIMARY KEY (id);


--
-- TOC entry 3880 (class 2606 OID 16502)
-- Name: arelationship arelationship_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arelationship
    ADD CONSTRAINT arelationship_pkey PRIMARY KEY (id);


--
-- TOC entry 3885 (class 2606 OID 16511)
-- Name: attrreleasepolicy attrreleasepolicy_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attrreleasepolicy
    ADD CONSTRAINT attrreleasepolicy_pkey PRIMARY KEY (id);


--
-- TOC entry 3887 (class 2606 OID 16518)
-- Name: attrrepo attrrepo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attrrepo
    ADD CONSTRAINT attrrepo_pkey PRIMARY KEY (id);


--
-- TOC entry 3889 (class 2606 OID 16523)
-- Name: auditconf auditconf_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditconf
    ADD CONSTRAINT auditconf_pkey PRIMARY KEY (id);


--
-- TOC entry 3891 (class 2606 OID 16530)
-- Name: auditevent auditevent_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditevent
    ADD CONSTRAINT auditevent_pkey PRIMARY KEY (id);


--
-- TOC entry 3893 (class 2606 OID 16537)
-- Name: authmodule authmodule_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.authmodule
    ADD CONSTRAINT authmodule_pkey PRIMARY KEY (id);


--
-- TOC entry 3895 (class 2606 OID 16544)
-- Name: authpolicy authpolicy_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.authpolicy
    ADD CONSTRAINT authpolicy_pkey PRIMARY KEY (id);


--
-- TOC entry 3897 (class 2606 OID 16551)
-- Name: authprofile authprofile_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.authprofile
    ADD CONSTRAINT authprofile_pkey PRIMARY KEY (id);


--
-- TOC entry 3901 (class 2606 OID 16560)
-- Name: casspclientapp casspclientapp_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.casspclientapp
    ADD CONSTRAINT casspclientapp_pkey PRIMARY KEY (id);


--
-- TOC entry 3909 (class 2606 OID 16573)
-- Name: confparam confparam_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.confparam
    ADD CONSTRAINT confparam_pkey PRIMARY KEY (id);


--
-- TOC entry 3911 (class 2606 OID 16580)
-- Name: conninstance conninstance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conninstance
    ADD CONSTRAINT conninstance_pkey PRIMARY KEY (id);


--
-- TOC entry 3915 (class 2606 OID 16587)
-- Name: delegation delegation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.delegation
    ADD CONSTRAINT delegation_pkey PRIMARY KEY (id);


--
-- TOC entry 3919 (class 2606 OID 16599)
-- Name: derschema derschema_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.derschema
    ADD CONSTRAINT derschema_pkey PRIMARY KEY (id);


--
-- TOC entry 3921 (class 2606 OID 16604)
-- Name: dynrealm dynrealm_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dynrealm
    ADD CONSTRAINT dynrealm_pkey PRIMARY KEY (id);


--
-- TOC entry 4388 (class 2606 OID 18690)
-- Name: dynrealmmembers dynrealmmembers_any_id_dynrealm_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dynrealmmembers
    ADD CONSTRAINT dynrealmmembers_any_id_dynrealm_id_key UNIQUE (any_id, dynrealm_id);


--
-- TOC entry 3923 (class 2606 OID 16611)
-- Name: dynrealmmembership dynrealmmembership_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dynrealmmembership
    ADD CONSTRAINT dynrealmmembership_pkey PRIMARY KEY (id);


--
-- TOC entry 4392 (class 2606 OID 18695)
-- Name: dynrolemembers dynrolemembers_any_id_role_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dynrolemembers
    ADD CONSTRAINT dynrolemembers_any_id_role_id_key UNIQUE (any_id, role_id);


--
-- TOC entry 3925 (class 2606 OID 16618)
-- Name: externalresource externalresource_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.externalresource
    ADD CONSTRAINT externalresource_pkey PRIMARY KEY (id);


--
-- TOC entry 3929 (class 2606 OID 16632)
-- Name: fiqlquery fiqlquery_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fiqlquery
    ADD CONSTRAINT fiqlquery_pkey PRIMARY KEY (id);


--
-- TOC entry 4255 (class 2606 OID 18202)
-- Name: flw_ru_batch_part flw_ru_batch_part_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flw_ru_batch_part
    ADD CONSTRAINT flw_ru_batch_part_pkey PRIMARY KEY (id_);


--
-- TOC entry 4252 (class 2606 OID 18194)
-- Name: flw_ru_batch flw_ru_batch_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flw_ru_batch
    ADD CONSTRAINT flw_ru_batch_pkey PRIMARY KEY (id_);


--
-- TOC entry 3933 (class 2606 OID 16641)
-- Name: formpropertydef formpropertydef_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.formpropertydef
    ADD CONSTRAINT formpropertydef_pkey PRIMARY KEY (id);


--
-- TOC entry 3935 (class 2606 OID 16646)
-- Name: grelationship grelationship_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grelationship
    ADD CONSTRAINT grelationship_pkey PRIMARY KEY (id);


--
-- TOC entry 3939 (class 2606 OID 16655)
-- Name: implementation implementation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.implementation
    ADD CONSTRAINT implementation_pkey PRIMARY KEY (id);


--
-- TOC entry 3941 (class 2606 OID 16662)
-- Name: inboundcorrelationruleentity inboundcorrelationruleentity_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inboundcorrelationruleentity
    ADD CONSTRAINT inboundcorrelationruleentity_pkey PRIMARY KEY (id);


--
-- TOC entry 3945 (class 2606 OID 16669)
-- Name: inboundpolicy inboundpolicy_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inboundpolicy
    ADD CONSTRAINT inboundpolicy_pkey PRIMARY KEY (id);


--
-- TOC entry 3947 (class 2606 OID 16676)
-- Name: jobstatus jobstatus_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobstatus
    ADD CONSTRAINT jobstatus_pkey PRIMARY KEY (id);


--
-- TOC entry 3949 (class 2606 OID 16683)
-- Name: linkedaccount linkedaccount_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.linkedaccount
    ADD CONSTRAINT linkedaccount_pkey PRIMARY KEY (id);


--
-- TOC entry 3953 (class 2606 OID 16692)
-- Name: livesynctask livesynctask_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.livesynctask
    ADD CONSTRAINT livesynctask_pkey PRIMARY KEY (id);


--
-- TOC entry 3961 (class 2606 OID 16708)
-- Name: livesynctaskexec livesynctaskexec_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.livesynctaskexec
    ADD CONSTRAINT livesynctaskexec_pkey PRIMARY KEY (id);


--
-- TOC entry 3963 (class 2606 OID 16715)
-- Name: macrotask macrotask_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.macrotask
    ADD CONSTRAINT macrotask_pkey PRIMARY KEY (id);


--
-- TOC entry 3967 (class 2606 OID 16724)
-- Name: macrotaskcommand macrotaskcommand_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.macrotaskcommand
    ADD CONSTRAINT macrotaskcommand_pkey PRIMARY KEY (id);


--
-- TOC entry 3969 (class 2606 OID 16731)
-- Name: macrotaskexec macrotaskexec_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.macrotaskexec
    ADD CONSTRAINT macrotaskexec_pkey PRIMARY KEY (id);


--
-- TOC entry 3971 (class 2606 OID 16738)
-- Name: mailtemplate mailtemplate_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mailtemplate
    ADD CONSTRAINT mailtemplate_pkey PRIMARY KEY (id);


--
-- TOC entry 3973 (class 2606 OID 16743)
-- Name: networkservice networkservice_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.networkservice
    ADD CONSTRAINT networkservice_pkey PRIMARY KEY (id);


--
-- TOC entry 3977 (class 2606 OID 16752)
-- Name: notification notification_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (id);


--
-- TOC entry 3979 (class 2606 OID 16759)
-- Name: notificationtask notificationtask_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificationtask
    ADD CONSTRAINT notificationtask_pkey PRIMARY KEY (id);


--
-- TOC entry 3982 (class 2606 OID 16766)
-- Name: notificationtaskexec notificationtaskexec_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificationtaskexec
    ADD CONSTRAINT notificationtaskexec_pkey PRIMARY KEY (id);


--
-- TOC entry 3985 (class 2606 OID 16773)
-- Name: oidcjwks oidcjwks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcjwks
    ADD CONSTRAINT oidcjwks_pkey PRIMARY KEY (id);


--
-- TOC entry 3987 (class 2606 OID 16780)
-- Name: oidcprovider oidcprovider_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcprovider
    ADD CONSTRAINT oidcprovider_pkey PRIMARY KEY (id);


--
-- TOC entry 3995 (class 2606 OID 16796)
-- Name: oidcrpclientapp oidcrpclientapp_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcrpclientapp
    ADD CONSTRAINT oidcrpclientapp_pkey PRIMARY KEY (id);


--
-- TOC entry 4003 (class 2606 OID 16809)
-- Name: oidcusertemplate oidcusertemplate_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcusertemplate
    ADD CONSTRAINT oidcusertemplate_pkey PRIMARY KEY (id);


--
-- TOC entry 4007 (class 2606 OID 16816)
-- Name: passwordpolicy passwordpolicy_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.passwordpolicy
    ADD CONSTRAINT passwordpolicy_pkey PRIMARY KEY (id);


--
-- TOC entry 4011 (class 2606 OID 16828)
-- Name: plainschema plainschema_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.plainschema
    ADD CONSTRAINT plainschema_pkey PRIMARY KEY (id);


--
-- TOC entry 4013 (class 2606 OID 16835)
-- Name: propagationpolicy propagationpolicy_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.propagationpolicy
    ADD CONSTRAINT propagationpolicy_pkey PRIMARY KEY (id);


--
-- TOC entry 4015 (class 2606 OID 16842)
-- Name: propagationtask propagationtask_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.propagationtask
    ADD CONSTRAINT propagationtask_pkey PRIMARY KEY (id);


--
-- TOC entry 4017 (class 2606 OID 16849)
-- Name: propagationtaskexec propagationtaskexec_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.propagationtaskexec
    ADD CONSTRAINT propagationtaskexec_pkey PRIMARY KEY (id);


--
-- TOC entry 4020 (class 2606 OID 16856)
-- Name: pulltask pulltask_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pulltask
    ADD CONSTRAINT pulltask_pkey PRIMARY KEY (id);


--
-- TOC entry 4026 (class 2606 OID 16870)
-- Name: pulltaskexec pulltaskexec_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pulltaskexec
    ADD CONSTRAINT pulltaskexec_pkey PRIMARY KEY (id);


--
-- TOC entry 4029 (class 2606 OID 16877)
-- Name: pushcorrelationruleentity pushcorrelationruleentity_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pushcorrelationruleentity
    ADD CONSTRAINT pushcorrelationruleentity_pkey PRIMARY KEY (id);


--
-- TOC entry 4033 (class 2606 OID 16884)
-- Name: pushpolicy pushpolicy_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pushpolicy
    ADD CONSTRAINT pushpolicy_pkey PRIMARY KEY (id);


--
-- TOC entry 4035 (class 2606 OID 16891)
-- Name: pushtask pushtask_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pushtask
    ADD CONSTRAINT pushtask_pkey PRIMARY KEY (id);


--
-- TOC entry 4041 (class 2606 OID 16905)
-- Name: pushtaskexec pushtaskexec_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pushtaskexec
    ADD CONSTRAINT pushtaskexec_pkey PRIMARY KEY (id);


--
-- TOC entry 4046 (class 2606 OID 16912)
-- Name: realm realm_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT realm_pkey PRIMARY KEY (id);


--
-- TOC entry 4057 (class 2606 OID 16936)
-- Name: relationshiptype relationshiptype_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.relationshiptype
    ADD CONSTRAINT relationshiptype_pkey PRIMARY KEY (id);


--
-- TOC entry 4059 (class 2606 OID 16943)
-- Name: remediation remediation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.remediation
    ADD CONSTRAINT remediation_pkey PRIMARY KEY (id);


--
-- TOC entry 4061 (class 2606 OID 16950)
-- Name: report report_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.report
    ADD CONSTRAINT report_pkey PRIMARY KEY (id);


--
-- TOC entry 4065 (class 2606 OID 16959)
-- Name: reportexec reportexec_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reportexec
    ADD CONSTRAINT reportexec_pkey PRIMARY KEY (id);


--
-- TOC entry 4067 (class 2606 OID 16969)
-- Name: saml2idpentity saml2idpentity_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2idpentity
    ADD CONSTRAINT saml2idpentity_pkey PRIMARY KEY (id);


--
-- TOC entry 4069 (class 2606 OID 16976)
-- Name: saml2sp4uiidp saml2sp4uiidp_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2sp4uiidp
    ADD CONSTRAINT saml2sp4uiidp_pkey PRIMARY KEY (id);


--
-- TOC entry 4075 (class 2606 OID 16987)
-- Name: saml2sp4uiusertemplate saml2sp4uiusertemplate_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2sp4uiusertemplate
    ADD CONSTRAINT saml2sp4uiusertemplate_pkey PRIMARY KEY (id);


--
-- TOC entry 4079 (class 2606 OID 16996)
-- Name: saml2spclientapp saml2spclientapp_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2spclientapp
    ADD CONSTRAINT saml2spclientapp_pkey PRIMARY KEY (id);


--
-- TOC entry 4087 (class 2606 OID 17009)
-- Name: schedtask schedtask_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schedtask
    ADD CONSTRAINT schedtask_pkey PRIMARY KEY (id);


--
-- TOC entry 4091 (class 2606 OID 17018)
-- Name: schedtaskexec schedtaskexec_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schedtaskexec
    ADD CONSTRAINT schedtaskexec_pkey PRIMARY KEY (id);


--
-- TOC entry 4094 (class 2606 OID 17023)
-- Name: securityquestion securityquestion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.securityquestion
    ADD CONSTRAINT securityquestion_pkey PRIMARY KEY (id);


--
-- TOC entry 4098 (class 2606 OID 17032)
-- Name: sraroute sraroute_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sraroute
    ADD CONSTRAINT sraroute_pkey PRIMARY KEY (id);


--
-- TOC entry 4102 (class 2606 OID 17041)
-- Name: syncopebatch syncopebatch_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopebatch
    ADD CONSTRAINT syncopebatch_pkey PRIMARY KEY (id);


--
-- TOC entry 4104 (class 2606 OID 17048)
-- Name: syncopedomain syncopedomain_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopedomain
    ADD CONSTRAINT syncopedomain_pkey PRIMARY KEY (id);


--
-- TOC entry 4108 (class 2606 OID 17055)
-- Name: syncopegroup syncopegroup_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopegroup
    ADD CONSTRAINT syncopegroup_pkey PRIMARY KEY (id);


--
-- TOC entry 4118 (class 2606 OID 17074)
-- Name: syncoperole syncoperole_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncoperole
    ADD CONSTRAINT syncoperole_pkey PRIMARY KEY (id);


--
-- TOC entry 4124 (class 2606 OID 17093)
-- Name: syncopeschema syncopeschema_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopeschema
    ADD CONSTRAINT syncopeschema_pkey PRIMARY KEY (id);


--
-- TOC entry 4127 (class 2606 OID 17100)
-- Name: syncopeuser syncopeuser_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopeuser
    ADD CONSTRAINT syncopeuser_pkey PRIMARY KEY (id);


--
-- TOC entry 4140 (class 2606 OID 17124)
-- Name: ticketexpirationpolicy ticketexpirationpolicy_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ticketexpirationpolicy
    ADD CONSTRAINT ticketexpirationpolicy_pkey PRIMARY KEY (id);


--
-- TOC entry 4142 (class 2606 OID 17129)
-- Name: typeextension typeextension_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.typeextension
    ADD CONSTRAINT typeextension_pkey PRIMARY KEY (id);


--
-- TOC entry 3838 (class 2606 OID 16415)
-- Name: accountpolicyrule u_ccntyrl_policy_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accountpolicyrule
    ADD CONSTRAINT u_ccntyrl_policy_id UNIQUE (policy_id, implementation_id);


--
-- TOC entry 3834 (class 2606 OID 16405)
-- Name: accesstoken u_ccsstkn_owner; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accesstoken
    ADD CONSTRAINT u_ccsstkn_owner UNIQUE (owner);


--
-- TOC entry 3913 (class 2606 OID 16582)
-- Name: conninstance u_cnnntnc_displayname; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conninstance
    ADD CONSTRAINT u_cnnntnc_displayname UNIQUE (displayname);


--
-- TOC entry 3903 (class 2606 OID 16562)
-- Name: casspclientapp u_cssptpp_clientappid; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.casspclientapp
    ADD CONSTRAINT u_cssptpp_clientappid UNIQUE (clientappid);


--
-- TOC entry 3905 (class 2606 OID 16564)
-- Name: casspclientapp u_cssptpp_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.casspclientapp
    ADD CONSTRAINT u_cssptpp_name UNIQUE (name);


--
-- TOC entry 3907 (class 2606 OID 16566)
-- Name: casspclientapp u_cssptpp_serviceid; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.casspclientapp
    ADD CONSTRAINT u_cssptpp_serviceid UNIQUE (serviceid);


--
-- TOC entry 3989 (class 2606 OID 16782)
-- Name: oidcprovider u_dcprvdr_clientid; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcprovider
    ADD CONSTRAINT u_dcprvdr_clientid UNIQUE (clientid);


--
-- TOC entry 3991 (class 2606 OID 16784)
-- Name: oidcprovider u_dcprvdr_clientsecret; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcprovider
    ADD CONSTRAINT u_dcprvdr_clientsecret UNIQUE (clientsecret);


--
-- TOC entry 3993 (class 2606 OID 16786)
-- Name: oidcprovider u_dcprvdr_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcprovider
    ADD CONSTRAINT u_dcprvdr_name UNIQUE (name);


--
-- TOC entry 3997 (class 2606 OID 16798)
-- Name: oidcrpclientapp u_dcrptpp_clientappid; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcrpclientapp
    ADD CONSTRAINT u_dcrptpp_clientappid UNIQUE (clientappid);


--
-- TOC entry 3999 (class 2606 OID 16802)
-- Name: oidcrpclientapp u_dcrptpp_clientid; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcrpclientapp
    ADD CONSTRAINT u_dcrptpp_clientid UNIQUE (clientid);


--
-- TOC entry 4001 (class 2606 OID 16800)
-- Name: oidcrpclientapp u_dcrptpp_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcrpclientapp
    ADD CONSTRAINT u_dcrptpp_name UNIQUE (name);


--
-- TOC entry 4005 (class 2606 OID 16811)
-- Name: oidcusertemplate u_dcsrplt_op_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcusertemplate
    ADD CONSTRAINT u_dcsrplt_op_id UNIQUE (op_id);


--
-- TOC entry 3917 (class 2606 OID 16589)
-- Name: delegation u_dlgtion_delegating_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.delegation
    ADD CONSTRAINT u_dlgtion_delegating_id UNIQUE (delegating_id, delegated_id);


--
-- TOC entry 3931 (class 2606 OID 16634)
-- Name: fiqlquery u_fqlqury_owner_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fiqlquery
    ADD CONSTRAINT u_fqlqury_owner_id UNIQUE (owner_id, name);


--
-- TOC entry 3937 (class 2606 OID 16648)
-- Name: grelationship u_grltshp_type_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grelationship
    ADD CONSTRAINT u_grltshp_type_id UNIQUE (type_id, group_id, anyobject_id);


--
-- TOC entry 3951 (class 2606 OID 16685)
-- Name: linkedaccount u_lnkdcnt_connobjectkeyvalue; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.linkedaccount
    ADD CONSTRAINT u_lnkdcnt_connobjectkeyvalue UNIQUE (connobjectkeyvalue, resource_id);


--
-- TOC entry 3959 (class 2606 OID 16701)
-- Name: livesynctaskaction u_lvsyctn_task_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.livesynctaskaction
    ADD CONSTRAINT u_lvsyctn_task_id UNIQUE (task_id, implementation_id);


--
-- TOC entry 3955 (class 2606 OID 16694)
-- Name: livesynctask u_lvsytsk_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.livesynctask
    ADD CONSTRAINT u_lvsytsk_name UNIQUE (name);


--
-- TOC entry 3957 (class 2606 OID 16696)
-- Name: livesynctask u_lvsytsk_resource_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.livesynctask
    ADD CONSTRAINT u_lvsytsk_resource_id UNIQUE (resource_id);


--
-- TOC entry 3965 (class 2606 OID 16717)
-- Name: macrotask u_mcrotsk_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.macrotask
    ADD CONSTRAINT u_mcrotsk_name UNIQUE (name);


--
-- TOC entry 3943 (class 2606 OID 16664)
-- Name: inboundcorrelationruleentity u_nbndtty_inboundpolicy_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inboundcorrelationruleentity
    ADD CONSTRAINT u_nbndtty_inboundpolicy_id UNIQUE (inboundpolicy_id, anytype_id);


--
-- TOC entry 3975 (class 2606 OID 16745)
-- Name: networkservice u_ntwrrvc_type; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.networkservice
    ADD CONSTRAINT u_ntwrrvc_type UNIQUE (type, address);


--
-- TOC entry 3848 (class 2606 OID 16436)
-- Name: anyabout u_nyabout_notification_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anyabout
    ADD CONSTRAINT u_nyabout_notification_id UNIQUE (notification_id, anytype_id);


--
-- TOC entry 3858 (class 2606 OID 16450)
-- Name: anyobject_anytypeclass u_nybjlss_anyobject_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anyobject_anytypeclass
    ADD CONSTRAINT u_nybjlss_anyobject_id UNIQUE (anyobject_id, anytypeclass_id);


--
-- TOC entry 3860 (class 2606 OID 16455)
-- Name: anyobject_externalresource u_nybjsrc_anyobject_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anyobject_externalresource
    ADD CONSTRAINT u_nybjsrc_anyobject_id UNIQUE (anyobject_id, resource_id);


--
-- TOC entry 3856 (class 2606 OID 16445)
-- Name: anyobject u_nyobjct_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anyobject
    ADD CONSTRAINT u_nyobjct_name UNIQUE (name, type_id);


--
-- TOC entry 3873 (class 2606 OID 16482)
-- Name: anytemplaterealm u_nytmrlm_realm_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anytemplaterealm
    ADD CONSTRAINT u_nytmrlm_realm_id UNIQUE (realm_id, anytype_id);


--
-- TOC entry 3864 (class 2606 OID 16464)
-- Name: anytemplatelivesynctask u_nytmtsk_livesynctask_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anytemplatelivesynctask
    ADD CONSTRAINT u_nytmtsk_livesynctask_id UNIQUE (livesynctask_id, anytype_id);


--
-- TOC entry 3869 (class 2606 OID 16473)
-- Name: anytemplatepulltask u_nytmtsk_pulltask_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anytemplatepulltask
    ADD CONSTRAINT u_nytmtsk_pulltask_id UNIQUE (pulltask_id, anytype_id);


--
-- TOC entry 4024 (class 2606 OID 16863)
-- Name: pulltaskaction u_plltctn_task_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pulltaskaction
    ADD CONSTRAINT u_plltctn_task_id UNIQUE (task_id, implementation_id);


--
-- TOC entry 4031 (class 2606 OID 16879)
-- Name: pushcorrelationruleentity u_pshctty_pushpolicy_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pushcorrelationruleentity
    ADD CONSTRAINT u_pshctty_pushpolicy_id UNIQUE (pushpolicy_id, anytype_id);


--
-- TOC entry 4039 (class 2606 OID 16898)
-- Name: pushtaskaction u_pshtctn_task_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pushtaskaction
    ADD CONSTRAINT u_pshtctn_task_id UNIQUE (task_id, implementation_id);


--
-- TOC entry 4009 (class 2606 OID 16821)
-- Name: passwordpolicyrule u_psswyrl_policy_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.passwordpolicyrule
    ADD CONSTRAINT u_psswyrl_policy_id UNIQUE (policy_id, implementation_id);


--
-- TOC entry 4022 (class 2606 OID 16858)
-- Name: pulltask u_pulltsk_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pulltask
    ADD CONSTRAINT u_pulltsk_name UNIQUE (name);


--
-- TOC entry 4037 (class 2606 OID 16893)
-- Name: pushtask u_pushtsk_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pushtask
    ADD CONSTRAINT u_pushtsk_name UNIQUE (name);


--
-- TOC entry 4049 (class 2606 OID 16914)
-- Name: realm u_realm_fullpath; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT u_realm_fullpath UNIQUE (fullpath);


--
-- TOC entry 4051 (class 2606 OID 16916)
-- Name: realm u_realm_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT u_realm_name UNIQUE (name, parent_id);


--
-- TOC entry 4063 (class 2606 OID 16952)
-- Name: report u_report_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.report
    ADD CONSTRAINT u_report_name UNIQUE (name);


--
-- TOC entry 4055 (class 2606 OID 16929)
-- Name: realm_externalresource u_rlm_src_realm_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_externalresource
    ADD CONSTRAINT u_rlm_src_realm_id UNIQUE (realm_id, resource_id);


--
-- TOC entry 4053 (class 2606 OID 16921)
-- Name: realmaction u_rlmcton_realm_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realmaction
    ADD CONSTRAINT u_rlmcton_realm_id UNIQUE (realm_id, implementation_id);


--
-- TOC entry 3883 (class 2606 OID 16504)
-- Name: arelationship u_rltnshp_type_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arelationship
    ADD CONSTRAINT u_rltnshp_type_id UNIQUE (type_id, left_anyobject_id, right_anyobject_id);


--
-- TOC entry 4154 (class 2606 OID 17153)
-- Name: urelationship u_rltnshp_type_id1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.urelationship
    ADD CONSTRAINT u_rltnshp_type_id1 UNIQUE (type_id, user_id, anyobject_id);


--
-- TOC entry 4089 (class 2606 OID 17011)
-- Name: schedtask u_schdtsk_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schedtask
    ADD CONSTRAINT u_schdtsk_name UNIQUE (name);


--
-- TOC entry 4096 (class 2606 OID 17025)
-- Name: securityquestion u_scrtstn_content; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.securityquestion
    ADD CONSTRAINT u_scrtstn_content UNIQUE (content);


--
-- TOC entry 4071 (class 2606 OID 16978)
-- Name: saml2sp4uiidp u_sml24dp_entityid; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2sp4uiidp
    ADD CONSTRAINT u_sml24dp_entityid UNIQUE (entityid);


--
-- TOC entry 4073 (class 2606 OID 16980)
-- Name: saml2sp4uiidp u_sml24dp_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2sp4uiidp
    ADD CONSTRAINT u_sml24dp_name UNIQUE (name);


--
-- TOC entry 4077 (class 2606 OID 16989)
-- Name: saml2sp4uiusertemplate u_sml2plt_idp_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2sp4uiusertemplate
    ADD CONSTRAINT u_sml2plt_idp_id UNIQUE (idp_id);


--
-- TOC entry 4081 (class 2606 OID 16998)
-- Name: saml2spclientapp u_sml2tpp_clientappid; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2spclientapp
    ADD CONSTRAINT u_sml2tpp_clientappid UNIQUE (clientappid);


--
-- TOC entry 4083 (class 2606 OID 17002)
-- Name: saml2spclientapp u_sml2tpp_entityid; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2spclientapp
    ADD CONSTRAINT u_sml2tpp_entityid UNIQUE (entityid);


--
-- TOC entry 4085 (class 2606 OID 17000)
-- Name: saml2spclientapp u_sml2tpp_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2spclientapp
    ADD CONSTRAINT u_sml2tpp_name UNIQUE (name);


--
-- TOC entry 4100 (class 2606 OID 17034)
-- Name: sraroute u_srroute_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sraroute
    ADD CONSTRAINT u_srroute_name UNIQUE (name);


--
-- TOC entry 4112 (class 2606 OID 17057)
-- Name: syncopegroup u_syncgrp_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopegroup
    ADD CONSTRAINT u_syncgrp_name UNIQUE (name);


--
-- TOC entry 4114 (class 2606 OID 17062)
-- Name: syncopegroup_anytypeclass u_synclss_group_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopegroup_anytypeclass
    ADD CONSTRAINT u_synclss_group_id UNIQUE (group_id, anytypeclass_id);


--
-- TOC entry 4134 (class 2606 OID 17107)
-- Name: syncopeuser_anytypeclass u_synclss_user_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopeuser_anytypeclass
    ADD CONSTRAINT u_synclss_user_id UNIQUE (user_id, anytypeclass_id);


--
-- TOC entry 4138 (class 2606 OID 17117)
-- Name: syncopeuser_syncoperole u_syncprl_user_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopeuser_syncoperole
    ADD CONSTRAINT u_syncprl_user_id UNIQUE (user_id, role_id);


--
-- TOC entry 4132 (class 2606 OID 17102)
-- Name: syncopeuser u_syncpsr_username; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopeuser
    ADD CONSTRAINT u_syncpsr_username UNIQUE (username);


--
-- TOC entry 4120 (class 2606 OID 17081)
-- Name: syncoperole_dynrealm u_syncrlm_role_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncoperole_dynrealm
    ADD CONSTRAINT u_syncrlm_role_id UNIQUE (role_id, dynamicrealm_id);


--
-- TOC entry 4122 (class 2606 OID 17086)
-- Name: syncoperole_realm u_syncrlm_role_id1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncoperole_realm
    ADD CONSTRAINT u_syncrlm_role_id1 UNIQUE (role_id, realm_id);


--
-- TOC entry 4116 (class 2606 OID 17067)
-- Name: syncopegroup_externalresource u_syncsrc_group_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopegroup_externalresource
    ADD CONSTRAINT u_syncsrc_group_id UNIQUE (group_id, resource_id);


--
-- TOC entry 4136 (class 2606 OID 17112)
-- Name: syncopeuser_externalresource u_syncsrc_user_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopeuser_externalresource
    ADD CONSTRAINT u_syncsrc_user_id UNIQUE (user_id, resource_id);


--
-- TOC entry 4146 (class 2606 OID 17136)
-- Name: typeextension_anytypeclass u_typxlss_typeextension_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.typeextension_anytypeclass
    ADD CONSTRAINT u_typxlss_typeextension_id UNIQUE (typeextension_id, anytypeclass_id);


--
-- TOC entry 4144 (class 2606 OID 17131)
-- Name: typeextension u_typxnsn_group_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.typeextension
    ADD CONSTRAINT u_typxnsn_group_id UNIQUE (group_id, anytype_id);


--
-- TOC entry 3899 (class 2606 OID 16553)
-- Name: authprofile u_uthprfl_owner; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.authprofile
    ADD CONSTRAINT u_uthprfl_owner UNIQUE (owner);


--
-- TOC entry 3927 (class 2606 OID 16625)
-- Name: externalresourcepropaction u_xtrnctn_resource_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.externalresourcepropaction
    ADD CONSTRAINT u_xtrnctn_resource_id UNIQUE (resource_id, implementation_id);


--
-- TOC entry 4396 (class 2606 OID 18700)
-- Name: udyngroupmembers udyngroupmembers_any_id_group_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.udyngroupmembers
    ADD CONSTRAINT udyngroupmembers_any_id_group_id_key UNIQUE (any_id, group_id);


--
-- TOC entry 4148 (class 2606 OID 17141)
-- Name: udyngroupmembership udyngroupmembership_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.udyngroupmembership
    ADD CONSTRAINT udyngroupmembership_pkey PRIMARY KEY (id);


--
-- TOC entry 4151 (class 2606 OID 17146)
-- Name: umembership umembership_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.umembership
    ADD CONSTRAINT umembership_pkey PRIMARY KEY (id);


--
-- TOC entry 4157 (class 2606 OID 17151)
-- Name: urelationship urelationship_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.urelationship
    ADD CONSTRAINT urelationship_pkey PRIMARY KEY (id);


--
-- TOC entry 4160 (class 2606 OID 17160)
-- Name: waconfigentry waconfigentry_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.waconfigentry
    ADD CONSTRAINT waconfigentry_pkey PRIMARY KEY (id);


--
-- TOC entry 4273 (class 1259 OID 18243)
-- Name: act_idx_act_hi_tsk_log_task; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_act_hi_tsk_log_task ON public.act_hi_tsk_log USING btree (task_id_);


--
-- TOC entry 4178 (class 1259 OID 18388)
-- Name: act_idx_athrz_procedef; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_athrz_procedef ON public.act_ru_identitylink USING btree (proc_def_id_);


--
-- TOC entry 4165 (class 1259 OID 18343)
-- Name: act_idx_bytear_depl; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_bytear_depl ON public.act_ge_bytearray USING btree (deployment_id_);


--
-- TOC entry 4230 (class 1259 OID 18118)
-- Name: act_idx_deadletter_job_correlation_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_deadletter_job_correlation_id ON public.act_ru_deadletter_job USING btree (correlation_id_);


--
-- TOC entry 4231 (class 1259 OID 18117)
-- Name: act_idx_deadletter_job_custom_values_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_deadletter_job_custom_values_id ON public.act_ru_deadletter_job USING btree (custom_values_id_);


--
-- TOC entry 4232 (class 1259 OID 18116)
-- Name: act_idx_deadletter_job_exception_stack_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_deadletter_job_exception_stack_id ON public.act_ru_deadletter_job USING btree (exception_stack_id_);


--
-- TOC entry 4233 (class 1259 OID 18484)
-- Name: act_idx_deadletter_job_execution_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_deadletter_job_execution_id ON public.act_ru_deadletter_job USING btree (execution_id_);


--
-- TOC entry 4234 (class 1259 OID 18496)
-- Name: act_idx_deadletter_job_proc_def_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_deadletter_job_proc_def_id ON public.act_ru_deadletter_job USING btree (proc_def_id_);


--
-- TOC entry 4235 (class 1259 OID 18490)
-- Name: act_idx_deadletter_job_process_instance_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_deadletter_job_process_instance_id ON public.act_ru_deadletter_job USING btree (process_instance_id_);


--
-- TOC entry 4236 (class 1259 OID 18181)
-- Name: act_idx_djob_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_djob_scope ON public.act_ru_deadletter_job USING btree (scope_id_, scope_type_);


--
-- TOC entry 4237 (class 1259 OID 18183)
-- Name: act_idx_djob_scope_def; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_djob_scope_def ON public.act_ru_deadletter_job USING btree (scope_definition_id_, scope_type_);


--
-- TOC entry 4238 (class 1259 OID 18182)
-- Name: act_idx_djob_sub_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_djob_sub_scope ON public.act_ru_deadletter_job USING btree (sub_scope_id_, scope_type_);


--
-- TOC entry 4243 (class 1259 OID 18184)
-- Name: act_idx_ejob_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ejob_scope ON public.act_ru_external_job USING btree (scope_id_, scope_type_);


--
-- TOC entry 4244 (class 1259 OID 18186)
-- Name: act_idx_ejob_scope_def; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ejob_scope_def ON public.act_ru_external_job USING btree (scope_definition_id_, scope_type_);


--
-- TOC entry 4245 (class 1259 OID 18185)
-- Name: act_idx_ejob_sub_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ejob_sub_scope ON public.act_ru_external_job USING btree (sub_scope_id_, scope_type_);


--
-- TOC entry 4166 (class 1259 OID 18021)
-- Name: act_idx_ent_lnk_ref_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ent_lnk_ref_scope ON public.act_ru_entitylink USING btree (ref_scope_id_, ref_scope_type_, link_type_);


--
-- TOC entry 4167 (class 1259 OID 18022)
-- Name: act_idx_ent_lnk_root_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ent_lnk_root_scope ON public.act_ru_entitylink USING btree (root_scope_id_, root_scope_type_, link_type_);


--
-- TOC entry 4168 (class 1259 OID 18020)
-- Name: act_idx_ent_lnk_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ent_lnk_scope ON public.act_ru_entitylink USING btree (scope_id_, scope_type_, link_type_);


--
-- TOC entry 4169 (class 1259 OID 18023)
-- Name: act_idx_ent_lnk_scope_def; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ent_lnk_scope_def ON public.act_ru_entitylink USING btree (scope_definition_id_, scope_type_, link_type_);


--
-- TOC entry 4290 (class 1259 OID 18279)
-- Name: act_idx_event_subscr; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_event_subscr ON public.act_ru_event_subscr USING btree (execution_id_);


--
-- TOC entry 4291 (class 1259 OID 18278)
-- Name: act_idx_event_subscr_config_; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_event_subscr_config_ ON public.act_ru_event_subscr USING btree (configuration_);


--
-- TOC entry 4292 (class 1259 OID 18280)
-- Name: act_idx_event_subscr_proc_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_event_subscr_proc_id ON public.act_ru_event_subscr USING btree (proc_inst_id_);


--
-- TOC entry 4293 (class 1259 OID 18281)
-- Name: act_idx_event_subscr_scoperef_; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_event_subscr_scoperef_ ON public.act_ru_event_subscr USING btree (scope_id_, scope_type_);


--
-- TOC entry 4303 (class 1259 OID 18364)
-- Name: act_idx_exe_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_exe_parent ON public.act_ru_execution USING btree (parent_id_);


--
-- TOC entry 4304 (class 1259 OID 18376)
-- Name: act_idx_exe_procdef; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_exe_procdef ON public.act_ru_execution USING btree (proc_def_id_);


--
-- TOC entry 4305 (class 1259 OID 18358)
-- Name: act_idx_exe_procinst; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_exe_procinst ON public.act_ru_execution USING btree (proc_inst_id_);


--
-- TOC entry 4306 (class 1259 OID 18340)
-- Name: act_idx_exe_root; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_exe_root ON public.act_ru_execution USING btree (root_proc_inst_id_);


--
-- TOC entry 4307 (class 1259 OID 18370)
-- Name: act_idx_exe_super; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_exe_super ON public.act_ru_execution USING btree (super_exec_);


--
-- TOC entry 4308 (class 1259 OID 18339)
-- Name: act_idx_exec_buskey; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_exec_buskey ON public.act_ru_execution USING btree (business_key_);


--
-- TOC entry 4309 (class 1259 OID 18341)
-- Name: act_idx_exec_ref_id_; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_exec_ref_id_ ON public.act_ru_execution USING btree (reference_id_);


--
-- TOC entry 4246 (class 1259 OID 18121)
-- Name: act_idx_external_job_correlation_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_external_job_correlation_id ON public.act_ru_external_job USING btree (correlation_id_);


--
-- TOC entry 4247 (class 1259 OID 18120)
-- Name: act_idx_external_job_custom_values_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_external_job_custom_values_id ON public.act_ru_external_job USING btree (custom_values_id_);


--
-- TOC entry 4248 (class 1259 OID 18119)
-- Name: act_idx_external_job_exception_stack_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_external_job_exception_stack_id ON public.act_ru_external_job USING btree (exception_stack_id_);


--
-- TOC entry 4342 (class 1259 OID 18584)
-- Name: act_idx_hi_act_inst_end; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_act_inst_end ON public.act_hi_actinst USING btree (end_time_);


--
-- TOC entry 4343 (class 1259 OID 18594)
-- Name: act_idx_hi_act_inst_exec; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_act_inst_exec ON public.act_hi_actinst USING btree (execution_id_, act_id_);


--
-- TOC entry 4344 (class 1259 OID 18593)
-- Name: act_idx_hi_act_inst_procinst; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_act_inst_procinst ON public.act_hi_actinst USING btree (proc_inst_id_, act_id_);


--
-- TOC entry 4345 (class 1259 OID 18583)
-- Name: act_idx_hi_act_inst_start; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_act_inst_start ON public.act_hi_actinst USING btree (start_time_);


--
-- TOC entry 4348 (class 1259 OID 18586)
-- Name: act_idx_hi_detail_act_inst; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_detail_act_inst ON public.act_hi_detail USING btree (act_inst_id_);


--
-- TOC entry 4349 (class 1259 OID 18588)
-- Name: act_idx_hi_detail_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_detail_name ON public.act_hi_detail USING btree (name_);


--
-- TOC entry 4350 (class 1259 OID 18585)
-- Name: act_idx_hi_detail_proc_inst; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_detail_proc_inst ON public.act_hi_detail USING btree (proc_inst_id_);


--
-- TOC entry 4351 (class 1259 OID 18589)
-- Name: act_idx_hi_detail_task_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_detail_task_id ON public.act_hi_detail USING btree (task_id_);


--
-- TOC entry 4352 (class 1259 OID 18587)
-- Name: act_idx_hi_detail_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_detail_time ON public.act_hi_detail USING btree (time_);


--
-- TOC entry 4174 (class 1259 OID 18032)
-- Name: act_idx_hi_ent_lnk_ref_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_ent_lnk_ref_scope ON public.act_hi_entitylink USING btree (ref_scope_id_, ref_scope_type_, link_type_);


--
-- TOC entry 4175 (class 1259 OID 18033)
-- Name: act_idx_hi_ent_lnk_root_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_ent_lnk_root_scope ON public.act_hi_entitylink USING btree (root_scope_id_, root_scope_type_, link_type_);


--
-- TOC entry 4176 (class 1259 OID 18031)
-- Name: act_idx_hi_ent_lnk_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_ent_lnk_scope ON public.act_hi_entitylink USING btree (scope_id_, scope_type_, link_type_);


--
-- TOC entry 4177 (class 1259 OID 18034)
-- Name: act_idx_hi_ent_lnk_scope_def; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_ent_lnk_scope_def ON public.act_hi_entitylink USING btree (scope_definition_id_, scope_type_, link_type_);


--
-- TOC entry 4190 (class 1259 OID 18596)
-- Name: act_idx_hi_ident_lnk_procinst; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_ident_lnk_procinst ON public.act_hi_identitylink USING btree (proc_inst_id_);


--
-- TOC entry 4191 (class 1259 OID 18055)
-- Name: act_idx_hi_ident_lnk_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_ident_lnk_scope ON public.act_hi_identitylink USING btree (scope_id_, scope_type_);


--
-- TOC entry 4192 (class 1259 OID 18057)
-- Name: act_idx_hi_ident_lnk_scope_def; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_ident_lnk_scope_def ON public.act_hi_identitylink USING btree (scope_definition_id_, scope_type_);


--
-- TOC entry 4193 (class 1259 OID 18056)
-- Name: act_idx_hi_ident_lnk_sub_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_ident_lnk_sub_scope ON public.act_hi_identitylink USING btree (sub_scope_id_, scope_type_);


--
-- TOC entry 4194 (class 1259 OID 18595)
-- Name: act_idx_hi_ident_lnk_task; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_ident_lnk_task ON public.act_hi_identitylink USING btree (task_id_);


--
-- TOC entry 4195 (class 1259 OID 18054)
-- Name: act_idx_hi_ident_lnk_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_ident_lnk_user ON public.act_hi_identitylink USING btree (user_id_);


--
-- TOC entry 4337 (class 1259 OID 18581)
-- Name: act_idx_hi_pro_i_buskey; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_pro_i_buskey ON public.act_hi_procinst USING btree (business_key_);


--
-- TOC entry 4338 (class 1259 OID 18580)
-- Name: act_idx_hi_pro_inst_end; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_pro_inst_end ON public.act_hi_procinst USING btree (end_time_);


--
-- TOC entry 4339 (class 1259 OID 18582)
-- Name: act_idx_hi_pro_super_procinst; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_pro_super_procinst ON public.act_hi_procinst USING btree (super_process_instance_id_);


--
-- TOC entry 4284 (class 1259 OID 18592)
-- Name: act_idx_hi_procvar_exe; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_procvar_exe ON public.act_hi_varinst USING btree (execution_id_);


--
-- TOC entry 4285 (class 1259 OID 18267)
-- Name: act_idx_hi_procvar_name_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_procvar_name_type ON public.act_hi_varinst USING btree (name_, var_type_);


--
-- TOC entry 4286 (class 1259 OID 18590)
-- Name: act_idx_hi_procvar_proc_inst; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_procvar_proc_inst ON public.act_hi_varinst USING btree (proc_inst_id_);


--
-- TOC entry 4287 (class 1259 OID 18591)
-- Name: act_idx_hi_procvar_task_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_procvar_task_id ON public.act_hi_varinst USING btree (task_id_);


--
-- TOC entry 4267 (class 1259 OID 18597)
-- Name: act_idx_hi_task_inst_procinst; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_task_inst_procinst ON public.act_hi_taskinst USING btree (proc_inst_id_);


--
-- TOC entry 4268 (class 1259 OID 18240)
-- Name: act_idx_hi_task_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_task_scope ON public.act_hi_taskinst USING btree (scope_id_, scope_type_);


--
-- TOC entry 4269 (class 1259 OID 18242)
-- Name: act_idx_hi_task_scope_def; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_task_scope_def ON public.act_hi_taskinst USING btree (scope_definition_id_, scope_type_);


--
-- TOC entry 4270 (class 1259 OID 18241)
-- Name: act_idx_hi_task_sub_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_task_sub_scope ON public.act_hi_taskinst USING btree (sub_scope_id_, scope_type_);


--
-- TOC entry 4288 (class 1259 OID 18268)
-- Name: act_idx_hi_var_scope_id_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_var_scope_id_type ON public.act_hi_varinst USING btree (scope_id_, scope_type_);


--
-- TOC entry 4289 (class 1259 OID 18269)
-- Name: act_idx_hi_var_sub_id_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_hi_var_sub_id_type ON public.act_hi_varinst USING btree (sub_scope_id_, scope_type_);


--
-- TOC entry 4179 (class 1259 OID 18043)
-- Name: act_idx_ident_lnk_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ident_lnk_group ON public.act_ru_identitylink USING btree (group_id_);


--
-- TOC entry 4180 (class 1259 OID 18044)
-- Name: act_idx_ident_lnk_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ident_lnk_scope ON public.act_ru_identitylink USING btree (scope_id_, scope_type_);


--
-- TOC entry 4181 (class 1259 OID 18046)
-- Name: act_idx_ident_lnk_scope_def; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ident_lnk_scope_def ON public.act_ru_identitylink USING btree (scope_definition_id_, scope_type_);


--
-- TOC entry 4182 (class 1259 OID 18045)
-- Name: act_idx_ident_lnk_sub_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ident_lnk_sub_scope ON public.act_ru_identitylink USING btree (sub_scope_id_, scope_type_);


--
-- TOC entry 4183 (class 1259 OID 18042)
-- Name: act_idx_ident_lnk_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ident_lnk_user ON public.act_ru_identitylink USING btree (user_id_);


--
-- TOC entry 4184 (class 1259 OID 18394)
-- Name: act_idx_idl_procinst; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_idl_procinst ON public.act_ru_identitylink USING btree (proc_inst_id_);


--
-- TOC entry 4196 (class 1259 OID 18108)
-- Name: act_idx_job_correlation_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_job_correlation_id ON public.act_ru_job USING btree (correlation_id_);


--
-- TOC entry 4197 (class 1259 OID 18107)
-- Name: act_idx_job_custom_values_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_job_custom_values_id ON public.act_ru_job USING btree (custom_values_id_);


--
-- TOC entry 4198 (class 1259 OID 18106)
-- Name: act_idx_job_exception_stack_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_job_exception_stack_id ON public.act_ru_job USING btree (exception_stack_id_);


--
-- TOC entry 4199 (class 1259 OID 18430)
-- Name: act_idx_job_execution_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_job_execution_id ON public.act_ru_job USING btree (execution_id_);


--
-- TOC entry 4200 (class 1259 OID 18442)
-- Name: act_idx_job_proc_def_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_job_proc_def_id ON public.act_ru_job USING btree (proc_def_id_);


--
-- TOC entry 4201 (class 1259 OID 18436)
-- Name: act_idx_job_process_instance_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_job_process_instance_id ON public.act_ru_job USING btree (process_instance_id_);


--
-- TOC entry 4202 (class 1259 OID 18172)
-- Name: act_idx_job_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_job_scope ON public.act_ru_job USING btree (scope_id_, scope_type_);


--
-- TOC entry 4203 (class 1259 OID 18174)
-- Name: act_idx_job_scope_def; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_job_scope_def ON public.act_ru_job USING btree (scope_definition_id_, scope_type_);


--
-- TOC entry 4204 (class 1259 OID 18173)
-- Name: act_idx_job_sub_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_job_sub_scope ON public.act_ru_job USING btree (sub_scope_id_, scope_type_);


--
-- TOC entry 4365 (class 1259 OID 18656)
-- Name: act_idx_memb_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_memb_group ON public.act_id_membership USING btree (group_id_);


--
-- TOC entry 4366 (class 1259 OID 18662)
-- Name: act_idx_memb_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_memb_user ON public.act_id_membership USING btree (user_id_);


--
-- TOC entry 4298 (class 1259 OID 18519)
-- Name: act_idx_model_deployment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_model_deployment ON public.act_re_model USING btree (deployment_id_);


--
-- TOC entry 4299 (class 1259 OID 18507)
-- Name: act_idx_model_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_model_source ON public.act_re_model USING btree (editor_source_value_id_);


--
-- TOC entry 4300 (class 1259 OID 18513)
-- Name: act_idx_model_source_extra; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_model_source_extra ON public.act_re_model USING btree (editor_source_extra_value_id_);


--
-- TOC entry 4379 (class 1259 OID 18675)
-- Name: act_idx_priv_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_priv_group ON public.act_id_priv_mapping USING btree (group_id_);


--
-- TOC entry 4380 (class 1259 OID 18668)
-- Name: act_idx_priv_mapping; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_priv_mapping ON public.act_id_priv_mapping USING btree (priv_id_);


--
-- TOC entry 4381 (class 1259 OID 18674)
-- Name: act_idx_priv_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_priv_user ON public.act_id_priv_mapping USING btree (user_id_);


--
-- TOC entry 4318 (class 1259 OID 18525)
-- Name: act_idx_procdef_info_json; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_procdef_info_json ON public.act_procdef_info USING btree (info_json_id_);


--
-- TOC entry 4319 (class 1259 OID 18531)
-- Name: act_idx_procdef_info_proc; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_procdef_info_proc ON public.act_procdef_info USING btree (proc_def_id_);


--
-- TOC entry 4324 (class 1259 OID 18345)
-- Name: act_idx_ru_acti_end; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ru_acti_end ON public.act_ru_actinst USING btree (end_time_);


--
-- TOC entry 4325 (class 1259 OID 18348)
-- Name: act_idx_ru_acti_exec; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ru_acti_exec ON public.act_ru_actinst USING btree (execution_id_);


--
-- TOC entry 4326 (class 1259 OID 18349)
-- Name: act_idx_ru_acti_exec_act; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ru_acti_exec_act ON public.act_ru_actinst USING btree (execution_id_, act_id_);


--
-- TOC entry 4327 (class 1259 OID 18346)
-- Name: act_idx_ru_acti_proc; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ru_acti_proc ON public.act_ru_actinst USING btree (proc_inst_id_);


--
-- TOC entry 4328 (class 1259 OID 18347)
-- Name: act_idx_ru_acti_proc_act; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ru_acti_proc_act ON public.act_ru_actinst USING btree (proc_inst_id_, act_id_);


--
-- TOC entry 4329 (class 1259 OID 18344)
-- Name: act_idx_ru_acti_start; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ru_acti_start ON public.act_ru_actinst USING btree (start_time_);


--
-- TOC entry 4330 (class 1259 OID 18350)
-- Name: act_idx_ru_acti_task; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ru_acti_task ON public.act_ru_actinst USING btree (task_id_);


--
-- TOC entry 4274 (class 1259 OID 18251)
-- Name: act_idx_ru_var_scope_id_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ru_var_scope_id_type ON public.act_ru_variable USING btree (scope_id_, scope_type_);


--
-- TOC entry 4275 (class 1259 OID 18252)
-- Name: act_idx_ru_var_sub_id_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_ru_var_sub_id_type ON public.act_ru_variable USING btree (sub_scope_id_, scope_type_);


--
-- TOC entry 4219 (class 1259 OID 18178)
-- Name: act_idx_sjob_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_sjob_scope ON public.act_ru_suspended_job USING btree (scope_id_, scope_type_);


--
-- TOC entry 4220 (class 1259 OID 18180)
-- Name: act_idx_sjob_scope_def; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_sjob_scope_def ON public.act_ru_suspended_job USING btree (scope_definition_id_, scope_type_);


--
-- TOC entry 4221 (class 1259 OID 18179)
-- Name: act_idx_sjob_sub_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_sjob_sub_scope ON public.act_ru_suspended_job USING btree (sub_scope_id_, scope_type_);


--
-- TOC entry 4222 (class 1259 OID 18115)
-- Name: act_idx_suspended_job_correlation_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_suspended_job_correlation_id ON public.act_ru_suspended_job USING btree (correlation_id_);


--
-- TOC entry 4223 (class 1259 OID 18114)
-- Name: act_idx_suspended_job_custom_values_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_suspended_job_custom_values_id ON public.act_ru_suspended_job USING btree (custom_values_id_);


--
-- TOC entry 4224 (class 1259 OID 18113)
-- Name: act_idx_suspended_job_exception_stack_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_suspended_job_exception_stack_id ON public.act_ru_suspended_job USING btree (exception_stack_id_);


--
-- TOC entry 4225 (class 1259 OID 18466)
-- Name: act_idx_suspended_job_execution_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_suspended_job_execution_id ON public.act_ru_suspended_job USING btree (execution_id_);


--
-- TOC entry 4226 (class 1259 OID 18478)
-- Name: act_idx_suspended_job_proc_def_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_suspended_job_proc_def_id ON public.act_ru_suspended_job USING btree (proc_def_id_);


--
-- TOC entry 4227 (class 1259 OID 18472)
-- Name: act_idx_suspended_job_process_instance_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_suspended_job_process_instance_id ON public.act_ru_suspended_job USING btree (process_instance_id_);


--
-- TOC entry 4256 (class 1259 OID 18217)
-- Name: act_idx_task_create; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_task_create ON public.act_ru_task USING btree (create_time_);


--
-- TOC entry 4257 (class 1259 OID 18400)
-- Name: act_idx_task_exec; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_task_exec ON public.act_ru_task USING btree (execution_id_);


--
-- TOC entry 4258 (class 1259 OID 18412)
-- Name: act_idx_task_procdef; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_task_procdef ON public.act_ru_task USING btree (proc_def_id_);


--
-- TOC entry 4259 (class 1259 OID 18406)
-- Name: act_idx_task_procinst; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_task_procinst ON public.act_ru_task USING btree (proc_inst_id_);


--
-- TOC entry 4260 (class 1259 OID 18218)
-- Name: act_idx_task_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_task_scope ON public.act_ru_task USING btree (scope_id_, scope_type_);


--
-- TOC entry 4261 (class 1259 OID 18220)
-- Name: act_idx_task_scope_def; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_task_scope_def ON public.act_ru_task USING btree (scope_definition_id_, scope_type_);


--
-- TOC entry 4262 (class 1259 OID 18219)
-- Name: act_idx_task_sub_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_task_sub_scope ON public.act_ru_task USING btree (sub_scope_id_, scope_type_);


--
-- TOC entry 4207 (class 1259 OID 18111)
-- Name: act_idx_timer_job_correlation_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_timer_job_correlation_id ON public.act_ru_timer_job USING btree (correlation_id_);


--
-- TOC entry 4208 (class 1259 OID 18110)
-- Name: act_idx_timer_job_custom_values_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_timer_job_custom_values_id ON public.act_ru_timer_job USING btree (custom_values_id_);


--
-- TOC entry 4209 (class 1259 OID 18112)
-- Name: act_idx_timer_job_duedate; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_timer_job_duedate ON public.act_ru_timer_job USING btree (duedate_);


--
-- TOC entry 4210 (class 1259 OID 18109)
-- Name: act_idx_timer_job_exception_stack_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_timer_job_exception_stack_id ON public.act_ru_timer_job USING btree (exception_stack_id_);


--
-- TOC entry 4211 (class 1259 OID 18448)
-- Name: act_idx_timer_job_execution_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_timer_job_execution_id ON public.act_ru_timer_job USING btree (execution_id_);


--
-- TOC entry 4212 (class 1259 OID 18460)
-- Name: act_idx_timer_job_proc_def_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_timer_job_proc_def_id ON public.act_ru_timer_job USING btree (proc_def_id_);


--
-- TOC entry 4213 (class 1259 OID 18454)
-- Name: act_idx_timer_job_process_instance_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_timer_job_process_instance_id ON public.act_ru_timer_job USING btree (process_instance_id_);


--
-- TOC entry 4214 (class 1259 OID 18175)
-- Name: act_idx_tjob_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_tjob_scope ON public.act_ru_timer_job USING btree (scope_id_, scope_type_);


--
-- TOC entry 4215 (class 1259 OID 18177)
-- Name: act_idx_tjob_scope_def; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_tjob_scope_def ON public.act_ru_timer_job USING btree (scope_definition_id_, scope_type_);


--
-- TOC entry 4216 (class 1259 OID 18176)
-- Name: act_idx_tjob_sub_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_tjob_sub_scope ON public.act_ru_timer_job USING btree (sub_scope_id_, scope_type_);


--
-- TOC entry 4185 (class 1259 OID 18382)
-- Name: act_idx_tskass_task; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_tskass_task ON public.act_ru_identitylink USING btree (task_id_);


--
-- TOC entry 4276 (class 1259 OID 18253)
-- Name: act_idx_var_bytearray; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_var_bytearray ON public.act_ru_variable USING btree (bytearray_id_);


--
-- TOC entry 4277 (class 1259 OID 18418)
-- Name: act_idx_var_exe; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_var_exe ON public.act_ru_variable USING btree (execution_id_);


--
-- TOC entry 4278 (class 1259 OID 18424)
-- Name: act_idx_var_procinst; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_var_procinst ON public.act_ru_variable USING btree (proc_inst_id_);


--
-- TOC entry 4279 (class 1259 OID 18342)
-- Name: act_idx_variable_task_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX act_idx_variable_task_id ON public.act_ru_variable USING btree (task_id_);


--
-- TOC entry 4382 (class 1259 OID 18757)
-- Name: adyngroupmembers_any_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX adyngroupmembers_any_id ON public.adyngroupmembers USING btree (any_id);


--
-- TOC entry 4385 (class 1259 OID 18758)
-- Name: adyngroupmembers_group_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX adyngroupmembers_group_id ON public.adyngroupmembers USING btree (group_id);


--
-- TOC entry 3841 (class 1259 OID 18759)
-- Name: amembership_anyobjectindex; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX amembership_anyobjectindex ON public.amembership USING btree (anyobject_id);


--
-- TOC entry 3842 (class 1259 OID 18760)
-- Name: amembership_groupindex; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX amembership_groupindex ON public.amembership USING btree (group_id);


--
-- TOC entry 3849 (class 1259 OID 18764)
-- Name: anyobject_lower_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX anyobject_lower_name ON public.anyobject USING btree (type_id, lower((name)::text));


--
-- TOC entry 3850 (class 1259 OID 18765)
-- Name: anyobject_name; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX anyobject_name ON public.anyobject USING btree (type_id, name);


--
-- TOC entry 3853 (class 1259 OID 18766)
-- Name: anyobject_plainattrs_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX anyobject_plainattrs_idx ON public.anyobject USING gin (plainattrs jsonb_path_ops);


--
-- TOC entry 3854 (class 1259 OID 18767)
-- Name: anyobject_realm_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX anyobject_realm_id ON public.anyobject USING btree (realm_id);


--
-- TOC entry 3878 (class 1259 OID 18761)
-- Name: arelationship_anyobjectindex; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX arelationship_anyobjectindex ON public.arelationship USING btree (left_anyobject_id);


--
-- TOC entry 3881 (class 1259 OID 18762)
-- Name: arelationship_rightindex; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX arelationship_rightindex ON public.arelationship USING btree (right_anyobject_id);


--
-- TOC entry 3867 (class 1259 OID 18763)
-- Name: atpulltask_pulltaskindex; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX atpulltask_pulltaskindex ON public.anytemplatepulltask USING btree (pulltask_id);


--
-- TOC entry 4386 (class 1259 OID 18768)
-- Name: dynrealmmembers_any_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX dynrealmmembers_any_id ON public.dynrealmmembers USING btree (any_id);


--
-- TOC entry 4389 (class 1259 OID 18769)
-- Name: dynrealmmembers_dynrealm_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX dynrealmmembers_dynrealm_id ON public.dynrealmmembers USING btree (dynrealm_id);


--
-- TOC entry 4390 (class 1259 OID 18770)
-- Name: dynrolemembers_any_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX dynrolemembers_any_id ON public.dynrolemembers USING btree (any_id);


--
-- TOC entry 4393 (class 1259 OID 18771)
-- Name: dynrolemembers_role_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX dynrolemembers_role_id ON public.dynrolemembers USING btree (role_id);


--
-- TOC entry 4253 (class 1259 OID 18203)
-- Name: flw_idx_batch_part; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flw_idx_batch_part ON public.flw_ru_batch_part USING btree (batch_id_);


--
-- TOC entry 4043 (class 1259 OID 18772)
-- Name: realm_fullpath_startswith; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX realm_fullpath_startswith ON public.realm USING gin (to_tsvector('english'::regconfig, (fullpath)::text));


--
-- TOC entry 4044 (class 1259 OID 18773)
-- Name: realm_parent_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX realm_parent_id ON public.realm USING btree (parent_id);


--
-- TOC entry 4047 (class 1259 OID 18774)
-- Name: realm_plainattrs_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX realm_plainattrs_idx ON public.realm USING gin (plainattrs jsonb_path_ops);


--
-- TOC entry 4105 (class 1259 OID 18775)
-- Name: syncopegroup_lower_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX syncopegroup_lower_name ON public.syncopegroup USING btree (lower((name)::text));


--
-- TOC entry 4106 (class 1259 OID 18776)
-- Name: syncopegroup_name; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX syncopegroup_name ON public.syncopegroup USING btree (name);


--
-- TOC entry 4109 (class 1259 OID 18777)
-- Name: syncopegroup_plainattrs_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX syncopegroup_plainattrs_idx ON public.syncopegroup USING gin (plainattrs jsonb_path_ops);


--
-- TOC entry 4110 (class 1259 OID 18778)
-- Name: syncopegroup_realm_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX syncopegroup_realm_id ON public.syncopegroup USING btree (realm_id);


--
-- TOC entry 4125 (class 1259 OID 18779)
-- Name: syncopeuser_lower_username; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX syncopeuser_lower_username ON public.syncopeuser USING btree (lower((username)::text));


--
-- TOC entry 4128 (class 1259 OID 18780)
-- Name: syncopeuser_plainattrs_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX syncopeuser_plainattrs_idx ON public.syncopeuser USING gin (plainattrs jsonb_path_ops);


--
-- TOC entry 4129 (class 1259 OID 18781)
-- Name: syncopeuser_realm_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX syncopeuser_realm_id ON public.syncopeuser USING btree (realm_id);


--
-- TOC entry 4130 (class 1259 OID 18782)
-- Name: syncopeuser_username; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX syncopeuser_username ON public.syncopeuser USING btree (username);


--
-- TOC entry 3980 (class 1259 OID 18788)
-- Name: task_executedindex; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX task_executedindex ON public.notificationtask USING btree (executed);


--
-- TOC entry 4018 (class 1259 OID 18783)
-- Name: taskexec1_taskidindex; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX taskexec1_taskidindex ON public.propagationtaskexec USING btree (task_id);


--
-- TOC entry 4027 (class 1259 OID 18784)
-- Name: taskexec2_taskidindex; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX taskexec2_taskidindex ON public.pulltaskexec USING btree (task_id);


--
-- TOC entry 4042 (class 1259 OID 18785)
-- Name: taskexec3_taskidindex; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX taskexec3_taskidindex ON public.pushtaskexec USING btree (task_id);


--
-- TOC entry 3983 (class 1259 OID 18786)
-- Name: taskexec4_taskidindex; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX taskexec4_taskidindex ON public.notificationtaskexec USING btree (task_id);


--
-- TOC entry 4092 (class 1259 OID 18787)
-- Name: taskexec5_taskidindex; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX taskexec5_taskidindex ON public.schedtaskexec USING btree (task_id);


--
-- TOC entry 4394 (class 1259 OID 18789)
-- Name: udyngroupmembers_any_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX udyngroupmembers_any_id ON public.udyngroupmembers USING btree (any_id);


--
-- TOC entry 4397 (class 1259 OID 18790)
-- Name: udyngroupmembers_group_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX udyngroupmembers_group_id ON public.udyngroupmembers USING btree (group_id);


--
-- TOC entry 4149 (class 1259 OID 18791)
-- Name: umembership_groupindex; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX umembership_groupindex ON public.umembership USING btree (group_id);


--
-- TOC entry 4152 (class 1259 OID 18792)
-- Name: umembership_userindex; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX umembership_userindex ON public.umembership USING btree (user_id);


--
-- TOC entry 4155 (class 1259 OID 18793)
-- Name: urelationship_leftindex; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX urelationship_leftindex ON public.urelationship USING btree (user_id);


--
-- TOC entry 4158 (class 1259 OID 18794)
-- Name: urelationship_rightindex; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX urelationship_rightindex ON public.urelationship USING btree (anyobject_id);


--
-- TOC entry 4398 (class 2606 OID 17166)
-- Name: accountpolicyrule accountpolicyrule_implementation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accountpolicyrule
    ADD CONSTRAINT accountpolicyrule_implementation_id_fkey FOREIGN KEY (implementation_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4399 (class 2606 OID 17161)
-- Name: accountpolicyrule accountpolicyrule_policy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accountpolicyrule
    ADD CONSTRAINT accountpolicyrule_policy_id_fkey FOREIGN KEY (policy_id) REFERENCES public.accountpolicy(id) DEFERRABLE;


--
-- TOC entry 4567 (class 2606 OID 18389)
-- Name: act_ru_identitylink act_fk_athrz_procedef; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_identitylink
    ADD CONSTRAINT act_fk_athrz_procedef FOREIGN KEY (proc_def_id_) REFERENCES public.act_re_procdef(id_);


--
-- TOC entry 4566 (class 2606 OID 18351)
-- Name: act_ge_bytearray act_fk_bytearr_depl; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ge_bytearray
    ADD CONSTRAINT act_fk_bytearr_depl FOREIGN KEY (deployment_id_) REFERENCES public.act_re_deployment(id_);


--
-- TOC entry 4585 (class 2606 OID 18157)
-- Name: act_ru_deadletter_job act_fk_deadletter_job_custom_values; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_deadletter_job
    ADD CONSTRAINT act_fk_deadletter_job_custom_values FOREIGN KEY (custom_values_id_) REFERENCES public.act_ge_bytearray(id_);


--
-- TOC entry 4586 (class 2606 OID 18152)
-- Name: act_ru_deadletter_job act_fk_deadletter_job_exception; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_deadletter_job
    ADD CONSTRAINT act_fk_deadletter_job_exception FOREIGN KEY (exception_stack_id_) REFERENCES public.act_ge_bytearray(id_);


--
-- TOC entry 4587 (class 2606 OID 18485)
-- Name: act_ru_deadletter_job act_fk_deadletter_job_execution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_deadletter_job
    ADD CONSTRAINT act_fk_deadletter_job_execution FOREIGN KEY (execution_id_) REFERENCES public.act_ru_execution(id_);


--
-- TOC entry 4588 (class 2606 OID 18497)
-- Name: act_ru_deadletter_job act_fk_deadletter_job_proc_def; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_deadletter_job
    ADD CONSTRAINT act_fk_deadletter_job_proc_def FOREIGN KEY (proc_def_id_) REFERENCES public.act_re_procdef(id_);


--
-- TOC entry 4589 (class 2606 OID 18491)
-- Name: act_ru_deadletter_job act_fk_deadletter_job_process_instance; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_deadletter_job
    ADD CONSTRAINT act_fk_deadletter_job_process_instance FOREIGN KEY (process_instance_id_) REFERENCES public.act_ru_execution(id_);


--
-- TOC entry 4599 (class 2606 OID 18502)
-- Name: act_ru_event_subscr act_fk_event_exec; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_event_subscr
    ADD CONSTRAINT act_fk_event_exec FOREIGN KEY (execution_id_) REFERENCES public.act_ru_execution(id_);


--
-- TOC entry 4603 (class 2606 OID 18365)
-- Name: act_ru_execution act_fk_exe_parent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_execution
    ADD CONSTRAINT act_fk_exe_parent FOREIGN KEY (parent_id_) REFERENCES public.act_ru_execution(id_);


--
-- TOC entry 4604 (class 2606 OID 18377)
-- Name: act_ru_execution act_fk_exe_procdef; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_execution
    ADD CONSTRAINT act_fk_exe_procdef FOREIGN KEY (proc_def_id_) REFERENCES public.act_re_procdef(id_);


--
-- TOC entry 4605 (class 2606 OID 18359)
-- Name: act_ru_execution act_fk_exe_procinst; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_execution
    ADD CONSTRAINT act_fk_exe_procinst FOREIGN KEY (proc_inst_id_) REFERENCES public.act_ru_execution(id_);


--
-- TOC entry 4606 (class 2606 OID 18371)
-- Name: act_ru_execution act_fk_exe_super; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_execution
    ADD CONSTRAINT act_fk_exe_super FOREIGN KEY (super_exec_) REFERENCES public.act_ru_execution(id_);


--
-- TOC entry 4590 (class 2606 OID 18167)
-- Name: act_ru_external_job act_fk_external_job_custom_values; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_external_job
    ADD CONSTRAINT act_fk_external_job_custom_values FOREIGN KEY (custom_values_id_) REFERENCES public.act_ge_bytearray(id_);


--
-- TOC entry 4591 (class 2606 OID 18162)
-- Name: act_ru_external_job act_fk_external_job_exception; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_external_job
    ADD CONSTRAINT act_fk_external_job_exception FOREIGN KEY (exception_stack_id_) REFERENCES public.act_ge_bytearray(id_);


--
-- TOC entry 4568 (class 2606 OID 18395)
-- Name: act_ru_identitylink act_fk_idl_procinst; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_identitylink
    ADD CONSTRAINT act_fk_idl_procinst FOREIGN KEY (proc_inst_id_) REFERENCES public.act_ru_execution(id_);


--
-- TOC entry 4607 (class 2606 OID 18526)
-- Name: act_procdef_info act_fk_info_json_ba; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_procdef_info
    ADD CONSTRAINT act_fk_info_json_ba FOREIGN KEY (info_json_id_) REFERENCES public.act_ge_bytearray(id_);


--
-- TOC entry 4608 (class 2606 OID 18532)
-- Name: act_procdef_info act_fk_info_procdef; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_procdef_info
    ADD CONSTRAINT act_fk_info_procdef FOREIGN KEY (proc_def_id_) REFERENCES public.act_re_procdef(id_);


--
-- TOC entry 4570 (class 2606 OID 18127)
-- Name: act_ru_job act_fk_job_custom_values; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_job
    ADD CONSTRAINT act_fk_job_custom_values FOREIGN KEY (custom_values_id_) REFERENCES public.act_ge_bytearray(id_);


--
-- TOC entry 4571 (class 2606 OID 18122)
-- Name: act_ru_job act_fk_job_exception; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_job
    ADD CONSTRAINT act_fk_job_exception FOREIGN KEY (exception_stack_id_) REFERENCES public.act_ge_bytearray(id_);


--
-- TOC entry 4572 (class 2606 OID 18431)
-- Name: act_ru_job act_fk_job_execution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_job
    ADD CONSTRAINT act_fk_job_execution FOREIGN KEY (execution_id_) REFERENCES public.act_ru_execution(id_);


--
-- TOC entry 4573 (class 2606 OID 18443)
-- Name: act_ru_job act_fk_job_proc_def; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_job
    ADD CONSTRAINT act_fk_job_proc_def FOREIGN KEY (proc_def_id_) REFERENCES public.act_re_procdef(id_);


--
-- TOC entry 4574 (class 2606 OID 18437)
-- Name: act_ru_job act_fk_job_process_instance; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_job
    ADD CONSTRAINT act_fk_job_process_instance FOREIGN KEY (process_instance_id_) REFERENCES public.act_ru_execution(id_);


--
-- TOC entry 4609 (class 2606 OID 18657)
-- Name: act_id_membership act_fk_memb_group; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_id_membership
    ADD CONSTRAINT act_fk_memb_group FOREIGN KEY (group_id_) REFERENCES public.act_id_group(id_);


--
-- TOC entry 4610 (class 2606 OID 18663)
-- Name: act_id_membership act_fk_memb_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_id_membership
    ADD CONSTRAINT act_fk_memb_user FOREIGN KEY (user_id_) REFERENCES public.act_id_user(id_);


--
-- TOC entry 4600 (class 2606 OID 18520)
-- Name: act_re_model act_fk_model_deployment; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_re_model
    ADD CONSTRAINT act_fk_model_deployment FOREIGN KEY (deployment_id_) REFERENCES public.act_re_deployment(id_);


--
-- TOC entry 4601 (class 2606 OID 18508)
-- Name: act_re_model act_fk_model_source; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_re_model
    ADD CONSTRAINT act_fk_model_source FOREIGN KEY (editor_source_value_id_) REFERENCES public.act_ge_bytearray(id_);


--
-- TOC entry 4602 (class 2606 OID 18514)
-- Name: act_re_model act_fk_model_source_extra; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_re_model
    ADD CONSTRAINT act_fk_model_source_extra FOREIGN KEY (editor_source_extra_value_id_) REFERENCES public.act_ge_bytearray(id_);


--
-- TOC entry 4611 (class 2606 OID 18669)
-- Name: act_id_priv_mapping act_fk_priv_mapping; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_id_priv_mapping
    ADD CONSTRAINT act_fk_priv_mapping FOREIGN KEY (priv_id_) REFERENCES public.act_id_priv(id_);


--
-- TOC entry 4580 (class 2606 OID 18147)
-- Name: act_ru_suspended_job act_fk_suspended_job_custom_values; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_suspended_job
    ADD CONSTRAINT act_fk_suspended_job_custom_values FOREIGN KEY (custom_values_id_) REFERENCES public.act_ge_bytearray(id_);


--
-- TOC entry 4581 (class 2606 OID 18142)
-- Name: act_ru_suspended_job act_fk_suspended_job_exception; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_suspended_job
    ADD CONSTRAINT act_fk_suspended_job_exception FOREIGN KEY (exception_stack_id_) REFERENCES public.act_ge_bytearray(id_);


--
-- TOC entry 4582 (class 2606 OID 18467)
-- Name: act_ru_suspended_job act_fk_suspended_job_execution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_suspended_job
    ADD CONSTRAINT act_fk_suspended_job_execution FOREIGN KEY (execution_id_) REFERENCES public.act_ru_execution(id_);


--
-- TOC entry 4583 (class 2606 OID 18479)
-- Name: act_ru_suspended_job act_fk_suspended_job_proc_def; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_suspended_job
    ADD CONSTRAINT act_fk_suspended_job_proc_def FOREIGN KEY (proc_def_id_) REFERENCES public.act_re_procdef(id_);


--
-- TOC entry 4584 (class 2606 OID 18473)
-- Name: act_ru_suspended_job act_fk_suspended_job_process_instance; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_suspended_job
    ADD CONSTRAINT act_fk_suspended_job_process_instance FOREIGN KEY (process_instance_id_) REFERENCES public.act_ru_execution(id_);


--
-- TOC entry 4593 (class 2606 OID 18401)
-- Name: act_ru_task act_fk_task_exe; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_task
    ADD CONSTRAINT act_fk_task_exe FOREIGN KEY (execution_id_) REFERENCES public.act_ru_execution(id_);


--
-- TOC entry 4594 (class 2606 OID 18413)
-- Name: act_ru_task act_fk_task_procdef; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_task
    ADD CONSTRAINT act_fk_task_procdef FOREIGN KEY (proc_def_id_) REFERENCES public.act_re_procdef(id_);


--
-- TOC entry 4595 (class 2606 OID 18407)
-- Name: act_ru_task act_fk_task_procinst; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_task
    ADD CONSTRAINT act_fk_task_procinst FOREIGN KEY (proc_inst_id_) REFERENCES public.act_ru_execution(id_);


--
-- TOC entry 4575 (class 2606 OID 18137)
-- Name: act_ru_timer_job act_fk_timer_job_custom_values; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_timer_job
    ADD CONSTRAINT act_fk_timer_job_custom_values FOREIGN KEY (custom_values_id_) REFERENCES public.act_ge_bytearray(id_);


--
-- TOC entry 4576 (class 2606 OID 18132)
-- Name: act_ru_timer_job act_fk_timer_job_exception; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_timer_job
    ADD CONSTRAINT act_fk_timer_job_exception FOREIGN KEY (exception_stack_id_) REFERENCES public.act_ge_bytearray(id_);


--
-- TOC entry 4577 (class 2606 OID 18449)
-- Name: act_ru_timer_job act_fk_timer_job_execution; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_timer_job
    ADD CONSTRAINT act_fk_timer_job_execution FOREIGN KEY (execution_id_) REFERENCES public.act_ru_execution(id_);


--
-- TOC entry 4578 (class 2606 OID 18461)
-- Name: act_ru_timer_job act_fk_timer_job_proc_def; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_timer_job
    ADD CONSTRAINT act_fk_timer_job_proc_def FOREIGN KEY (proc_def_id_) REFERENCES public.act_re_procdef(id_);


--
-- TOC entry 4579 (class 2606 OID 18455)
-- Name: act_ru_timer_job act_fk_timer_job_process_instance; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_timer_job
    ADD CONSTRAINT act_fk_timer_job_process_instance FOREIGN KEY (process_instance_id_) REFERENCES public.act_ru_execution(id_);


--
-- TOC entry 4569 (class 2606 OID 18383)
-- Name: act_ru_identitylink act_fk_tskass_task; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_identitylink
    ADD CONSTRAINT act_fk_tskass_task FOREIGN KEY (task_id_) REFERENCES public.act_ru_task(id_);


--
-- TOC entry 4596 (class 2606 OID 18254)
-- Name: act_ru_variable act_fk_var_bytearray; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_variable
    ADD CONSTRAINT act_fk_var_bytearray FOREIGN KEY (bytearray_id_) REFERENCES public.act_ge_bytearray(id_);


--
-- TOC entry 4597 (class 2606 OID 18419)
-- Name: act_ru_variable act_fk_var_exe; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_variable
    ADD CONSTRAINT act_fk_var_exe FOREIGN KEY (execution_id_) REFERENCES public.act_ru_execution(id_);


--
-- TOC entry 4598 (class 2606 OID 18425)
-- Name: act_ru_variable act_fk_var_procinst; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.act_ru_variable
    ADD CONSTRAINT act_fk_var_procinst FOREIGN KEY (proc_inst_id_) REFERENCES public.act_ru_execution(id_);


--
-- TOC entry 4400 (class 2606 OID 17176)
-- Name: adyngroupmembership adyngroupmembership_anytype_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.adyngroupmembership
    ADD CONSTRAINT adyngroupmembership_anytype_id_fkey FOREIGN KEY (anytype_id) REFERENCES public.anytype(id) DEFERRABLE;


--
-- TOC entry 4401 (class 2606 OID 17171)
-- Name: adyngroupmembership adyngroupmembership_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.adyngroupmembership
    ADD CONSTRAINT adyngroupmembership_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.syncopegroup(id) DEFERRABLE;


--
-- TOC entry 4402 (class 2606 OID 17181)
-- Name: amembership amembership_anyobject_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.amembership
    ADD CONSTRAINT amembership_anyobject_id_fkey FOREIGN KEY (anyobject_id) REFERENCES public.anyobject(id) DEFERRABLE;


--
-- TOC entry 4403 (class 2606 OID 17186)
-- Name: amembership amembership_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.amembership
    ADD CONSTRAINT amembership_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.syncopegroup(id) DEFERRABLE;


--
-- TOC entry 4404 (class 2606 OID 17191)
-- Name: anyabout anyabout_anytype_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anyabout
    ADD CONSTRAINT anyabout_anytype_id_fkey FOREIGN KEY (anytype_id) REFERENCES public.anytype(id) DEFERRABLE;


--
-- TOC entry 4405 (class 2606 OID 17196)
-- Name: anyabout anyabout_notification_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anyabout
    ADD CONSTRAINT anyabout_notification_id_fkey FOREIGN KEY (notification_id) REFERENCES public.notification(id) DEFERRABLE;


--
-- TOC entry 4408 (class 2606 OID 17211)
-- Name: anyobject_anytypeclass anyobject_anytypeclass_anyobject_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anyobject_anytypeclass
    ADD CONSTRAINT anyobject_anytypeclass_anyobject_id_fkey FOREIGN KEY (anyobject_id) REFERENCES public.anyobject(id) DEFERRABLE;


--
-- TOC entry 4409 (class 2606 OID 17216)
-- Name: anyobject_anytypeclass anyobject_anytypeclass_anytypeclass_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anyobject_anytypeclass
    ADD CONSTRAINT anyobject_anytypeclass_anytypeclass_id_fkey FOREIGN KEY (anytypeclass_id) REFERENCES public.anytypeclass(id) DEFERRABLE;


--
-- TOC entry 4410 (class 2606 OID 17221)
-- Name: anyobject_externalresource anyobject_externalresource_anyobject_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anyobject_externalresource
    ADD CONSTRAINT anyobject_externalresource_anyobject_id_fkey FOREIGN KEY (anyobject_id) REFERENCES public.anyobject(id) DEFERRABLE;


--
-- TOC entry 4411 (class 2606 OID 17226)
-- Name: anyobject_externalresource anyobject_externalresource_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anyobject_externalresource
    ADD CONSTRAINT anyobject_externalresource_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES public.externalresource(id) DEFERRABLE;


--
-- TOC entry 4406 (class 2606 OID 17201)
-- Name: anyobject anyobject_realm_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anyobject
    ADD CONSTRAINT anyobject_realm_id_fkey FOREIGN KEY (realm_id) REFERENCES public.realm(id) DEFERRABLE;


--
-- TOC entry 4407 (class 2606 OID 17206)
-- Name: anyobject anyobject_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anyobject
    ADD CONSTRAINT anyobject_type_id_fkey FOREIGN KEY (type_id) REFERENCES public.anytype(id) DEFERRABLE;


--
-- TOC entry 4412 (class 2606 OID 17236)
-- Name: anytemplatelivesynctask anytemplatelivesynctask_anytype_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anytemplatelivesynctask
    ADD CONSTRAINT anytemplatelivesynctask_anytype_id_fkey FOREIGN KEY (anytype_id) REFERENCES public.anytype(id) DEFERRABLE;


--
-- TOC entry 4413 (class 2606 OID 17231)
-- Name: anytemplatelivesynctask anytemplatelivesynctask_livesynctask_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anytemplatelivesynctask
    ADD CONSTRAINT anytemplatelivesynctask_livesynctask_id_fkey FOREIGN KEY (livesynctask_id) REFERENCES public.livesynctask(id) DEFERRABLE;


--
-- TOC entry 4414 (class 2606 OID 17241)
-- Name: anytemplatepulltask anytemplatepulltask_anytype_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anytemplatepulltask
    ADD CONSTRAINT anytemplatepulltask_anytype_id_fkey FOREIGN KEY (anytype_id) REFERENCES public.anytype(id) DEFERRABLE;


--
-- TOC entry 4415 (class 2606 OID 17246)
-- Name: anytemplatepulltask anytemplatepulltask_pulltask_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anytemplatepulltask
    ADD CONSTRAINT anytemplatepulltask_pulltask_id_fkey FOREIGN KEY (pulltask_id) REFERENCES public.pulltask(id) DEFERRABLE;


--
-- TOC entry 4416 (class 2606 OID 17256)
-- Name: anytemplaterealm anytemplaterealm_anytype_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anytemplaterealm
    ADD CONSTRAINT anytemplaterealm_anytype_id_fkey FOREIGN KEY (anytype_id) REFERENCES public.anytype(id) DEFERRABLE;


--
-- TOC entry 4417 (class 2606 OID 17251)
-- Name: anytemplaterealm anytemplaterealm_realm_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anytemplaterealm
    ADD CONSTRAINT anytemplaterealm_realm_id_fkey FOREIGN KEY (realm_id) REFERENCES public.realm(id) DEFERRABLE;


--
-- TOC entry 4418 (class 2606 OID 17261)
-- Name: anytype_anytypeclass anytype_anytypeclass_anytype_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anytype_anytypeclass
    ADD CONSTRAINT anytype_anytypeclass_anytype_id_fkey FOREIGN KEY (anytype_id) REFERENCES public.anytype(id) DEFERRABLE;


--
-- TOC entry 4419 (class 2606 OID 17266)
-- Name: anytype_anytypeclass anytype_anytypeclass_anytypeclass_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.anytype_anytypeclass
    ADD CONSTRAINT anytype_anytypeclass_anytypeclass_id_fkey FOREIGN KEY (anytypeclass_id) REFERENCES public.anytypeclass(id) DEFERRABLE;


--
-- TOC entry 4420 (class 2606 OID 17271)
-- Name: arelationship arelationship_left_anyobject_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arelationship
    ADD CONSTRAINT arelationship_left_anyobject_id_fkey FOREIGN KEY (left_anyobject_id) REFERENCES public.anyobject(id) DEFERRABLE;


--
-- TOC entry 4421 (class 2606 OID 17276)
-- Name: arelationship arelationship_right_anyobject_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arelationship
    ADD CONSTRAINT arelationship_right_anyobject_id_fkey FOREIGN KEY (right_anyobject_id) REFERENCES public.anyobject(id) DEFERRABLE;


--
-- TOC entry 4422 (class 2606 OID 17281)
-- Name: arelationship arelationship_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arelationship
    ADD CONSTRAINT arelationship_type_id_fkey FOREIGN KEY (type_id) REFERENCES public.relationshiptype(id) DEFERRABLE;


--
-- TOC entry 4423 (class 2606 OID 17286)
-- Name: casspclientapp casspclientapp_accesspolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.casspclientapp
    ADD CONSTRAINT casspclientapp_accesspolicy_id_fkey FOREIGN KEY (accesspolicy_id) REFERENCES public.accesspolicy(id) DEFERRABLE;


--
-- TOC entry 4424 (class 2606 OID 17291)
-- Name: casspclientapp casspclientapp_attrreleasepolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.casspclientapp
    ADD CONSTRAINT casspclientapp_attrreleasepolicy_id_fkey FOREIGN KEY (attrreleasepolicy_id) REFERENCES public.attrreleasepolicy(id) DEFERRABLE;


--
-- TOC entry 4425 (class 2606 OID 17296)
-- Name: casspclientapp casspclientapp_authpolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.casspclientapp
    ADD CONSTRAINT casspclientapp_authpolicy_id_fkey FOREIGN KEY (authpolicy_id) REFERENCES public.authpolicy(id) DEFERRABLE;


--
-- TOC entry 4426 (class 2606 OID 17301)
-- Name: casspclientapp casspclientapp_realm_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.casspclientapp
    ADD CONSTRAINT casspclientapp_realm_id_fkey FOREIGN KEY (realm_id) REFERENCES public.realm(id) DEFERRABLE;


--
-- TOC entry 4427 (class 2606 OID 17306)
-- Name: casspclientapp casspclientapp_ticketexpirationpolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.casspclientapp
    ADD CONSTRAINT casspclientapp_ticketexpirationpolicy_id_fkey FOREIGN KEY (ticketexpirationpolicy_id) REFERENCES public.ticketexpirationpolicy(id) DEFERRABLE;


--
-- TOC entry 4428 (class 2606 OID 17311)
-- Name: conninstance conninstance_adminrealm_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conninstance
    ADD CONSTRAINT conninstance_adminrealm_id_fkey FOREIGN KEY (adminrealm_id) REFERENCES public.realm(id) DEFERRABLE;


--
-- TOC entry 4429 (class 2606 OID 17316)
-- Name: delegation delegation_delegated_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.delegation
    ADD CONSTRAINT delegation_delegated_id_fkey FOREIGN KEY (delegated_id) REFERENCES public.syncopeuser(id) DEFERRABLE;


--
-- TOC entry 4430 (class 2606 OID 17321)
-- Name: delegation delegation_delegating_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.delegation
    ADD CONSTRAINT delegation_delegating_id_fkey FOREIGN KEY (delegating_id) REFERENCES public.syncopeuser(id) DEFERRABLE;


--
-- TOC entry 4431 (class 2606 OID 17326)
-- Name: delegation_syncoperole delegation_syncoperole_jpadelegation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.delegation_syncoperole
    ADD CONSTRAINT delegation_syncoperole_jpadelegation_id_fkey FOREIGN KEY (jpadelegation_id) REFERENCES public.delegation(id) DEFERRABLE;


--
-- TOC entry 4432 (class 2606 OID 17331)
-- Name: delegation_syncoperole delegation_syncoperole_roles_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.delegation_syncoperole
    ADD CONSTRAINT delegation_syncoperole_roles_id_fkey FOREIGN KEY (roles_id) REFERENCES public.syncoperole(id) DEFERRABLE;


--
-- TOC entry 4433 (class 2606 OID 17341)
-- Name: derschema derschema_anytypeclass_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.derschema
    ADD CONSTRAINT derschema_anytypeclass_id_fkey FOREIGN KEY (anytypeclass_id) REFERENCES public.anytypeclass(id) DEFERRABLE;


--
-- TOC entry 4434 (class 2606 OID 17336)
-- Name: derschema derschema_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.derschema
    ADD CONSTRAINT derschema_id_fkey FOREIGN KEY (id) REFERENCES public.syncopeschema(id) DEFERRABLE;


--
-- TOC entry 4435 (class 2606 OID 17351)
-- Name: dynrealmmembership dynrealmmembership_anytype_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dynrealmmembership
    ADD CONSTRAINT dynrealmmembership_anytype_id_fkey FOREIGN KEY (anytype_id) REFERENCES public.anytype(id) DEFERRABLE;


--
-- TOC entry 4436 (class 2606 OID 17346)
-- Name: dynrealmmembership dynrealmmembership_dynrealm_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dynrealmmembership
    ADD CONSTRAINT dynrealmmembership_dynrealm_id_fkey FOREIGN KEY (dynrealm_id) REFERENCES public.dynrealm(id) DEFERRABLE;


--
-- TOC entry 4437 (class 2606 OID 17356)
-- Name: externalresource externalresource_accountpolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.externalresource
    ADD CONSTRAINT externalresource_accountpolicy_id_fkey FOREIGN KEY (accountpolicy_id) REFERENCES public.accountpolicy(id) DEFERRABLE;


--
-- TOC entry 4438 (class 2606 OID 17361)
-- Name: externalresource externalresource_connector_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.externalresource
    ADD CONSTRAINT externalresource_connector_id_fkey FOREIGN KEY (connector_id) REFERENCES public.conninstance(id) DEFERRABLE;


--
-- TOC entry 4439 (class 2606 OID 17366)
-- Name: externalresource externalresource_inboundpolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.externalresource
    ADD CONSTRAINT externalresource_inboundpolicy_id_fkey FOREIGN KEY (inboundpolicy_id) REFERENCES public.inboundpolicy(id) DEFERRABLE;


--
-- TOC entry 4440 (class 2606 OID 17371)
-- Name: externalresource externalresource_passwordpolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.externalresource
    ADD CONSTRAINT externalresource_passwordpolicy_id_fkey FOREIGN KEY (passwordpolicy_id) REFERENCES public.passwordpolicy(id) DEFERRABLE;


--
-- TOC entry 4441 (class 2606 OID 17376)
-- Name: externalresource externalresource_propagationpolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.externalresource
    ADD CONSTRAINT externalresource_propagationpolicy_id_fkey FOREIGN KEY (propagationpolicy_id) REFERENCES public.propagationpolicy(id) DEFERRABLE;


--
-- TOC entry 4442 (class 2606 OID 17381)
-- Name: externalresource externalresource_provisionsorter_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.externalresource
    ADD CONSTRAINT externalresource_provisionsorter_id_fkey FOREIGN KEY (provisionsorter_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4443 (class 2606 OID 17386)
-- Name: externalresource externalresource_pushpolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.externalresource
    ADD CONSTRAINT externalresource_pushpolicy_id_fkey FOREIGN KEY (pushpolicy_id) REFERENCES public.pushpolicy(id) DEFERRABLE;


--
-- TOC entry 4444 (class 2606 OID 17396)
-- Name: externalresourcepropaction externalresourcepropaction_implementation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.externalresourcepropaction
    ADD CONSTRAINT externalresourcepropaction_implementation_id_fkey FOREIGN KEY (implementation_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4445 (class 2606 OID 17391)
-- Name: externalresourcepropaction externalresourcepropaction_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.externalresourcepropaction
    ADD CONSTRAINT externalresourcepropaction_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES public.externalresource(id) DEFERRABLE;


--
-- TOC entry 4446 (class 2606 OID 17401)
-- Name: fiqlquery fiqlquery_owner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fiqlquery
    ADD CONSTRAINT fiqlquery_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES public.syncopeuser(id) DEFERRABLE;


--
-- TOC entry 4592 (class 2606 OID 18204)
-- Name: flw_ru_batch_part flw_fk_batch_part_parent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flw_ru_batch_part
    ADD CONSTRAINT flw_fk_batch_part_parent FOREIGN KEY (batch_id_) REFERENCES public.flw_ru_batch(id_);


--
-- TOC entry 4447 (class 2606 OID 17406)
-- Name: formpropertydef formpropertydef_macrotask_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.formpropertydef
    ADD CONSTRAINT formpropertydef_macrotask_id_fkey FOREIGN KEY (macrotask_id) REFERENCES public.macrotask(id) DEFERRABLE;


--
-- TOC entry 4448 (class 2606 OID 17416)
-- Name: grelationship grelationship_anyobject_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grelationship
    ADD CONSTRAINT grelationship_anyobject_id_fkey FOREIGN KEY (anyobject_id) REFERENCES public.anyobject(id) DEFERRABLE;


--
-- TOC entry 4449 (class 2606 OID 17411)
-- Name: grelationship grelationship_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grelationship
    ADD CONSTRAINT grelationship_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.syncopegroup(id) DEFERRABLE;


--
-- TOC entry 4450 (class 2606 OID 17421)
-- Name: grelationship grelationship_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grelationship
    ADD CONSTRAINT grelationship_type_id_fkey FOREIGN KEY (type_id) REFERENCES public.relationshiptype(id) DEFERRABLE;


--
-- TOC entry 4451 (class 2606 OID 17431)
-- Name: inboundcorrelationruleentity inboundcorrelationruleentity_anytype_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inboundcorrelationruleentity
    ADD CONSTRAINT inboundcorrelationruleentity_anytype_id_fkey FOREIGN KEY (anytype_id) REFERENCES public.anytype(id) DEFERRABLE;


--
-- TOC entry 4452 (class 2606 OID 17436)
-- Name: inboundcorrelationruleentity inboundcorrelationruleentity_implementation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inboundcorrelationruleentity
    ADD CONSTRAINT inboundcorrelationruleentity_implementation_id_fkey FOREIGN KEY (implementation_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4453 (class 2606 OID 17426)
-- Name: inboundcorrelationruleentity inboundcorrelationruleentity_inboundpolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inboundcorrelationruleentity
    ADD CONSTRAINT inboundcorrelationruleentity_inboundpolicy_id_fkey FOREIGN KEY (inboundpolicy_id) REFERENCES public.inboundpolicy(id) DEFERRABLE;


--
-- TOC entry 4454 (class 2606 OID 17441)
-- Name: linkedaccount linkedaccount_owner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.linkedaccount
    ADD CONSTRAINT linkedaccount_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES public.syncopeuser(id) DEFERRABLE;


--
-- TOC entry 4455 (class 2606 OID 17446)
-- Name: linkedaccount linkedaccount_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.linkedaccount
    ADD CONSTRAINT linkedaccount_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES public.externalresource(id) DEFERRABLE;


--
-- TOC entry 4456 (class 2606 OID 17461)
-- Name: livesynctask livesynctask_destinationrealm_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.livesynctask
    ADD CONSTRAINT livesynctask_destinationrealm_id_fkey FOREIGN KEY (destinationrealm_id) REFERENCES public.realm(id) DEFERRABLE;


--
-- TOC entry 4457 (class 2606 OID 17451)
-- Name: livesynctask livesynctask_jobdelegate_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.livesynctask
    ADD CONSTRAINT livesynctask_jobdelegate_id_fkey FOREIGN KEY (jobdelegate_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4458 (class 2606 OID 17466)
-- Name: livesynctask livesynctask_livesyncdeltamapper_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.livesynctask
    ADD CONSTRAINT livesynctask_livesyncdeltamapper_id_fkey FOREIGN KEY (livesyncdeltamapper_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4459 (class 2606 OID 17456)
-- Name: livesynctask livesynctask_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.livesynctask
    ADD CONSTRAINT livesynctask_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES public.externalresource(id) DEFERRABLE;


--
-- TOC entry 4460 (class 2606 OID 17476)
-- Name: livesynctaskaction livesynctaskaction_implementation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.livesynctaskaction
    ADD CONSTRAINT livesynctaskaction_implementation_id_fkey FOREIGN KEY (implementation_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4461 (class 2606 OID 17471)
-- Name: livesynctaskaction livesynctaskaction_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.livesynctaskaction
    ADD CONSTRAINT livesynctaskaction_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.livesynctask(id) DEFERRABLE;


--
-- TOC entry 4462 (class 2606 OID 17481)
-- Name: livesynctaskexec livesynctaskexec_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.livesynctaskexec
    ADD CONSTRAINT livesynctaskexec_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.livesynctask(id) DEFERRABLE;


--
-- TOC entry 4463 (class 2606 OID 17486)
-- Name: macrotask macrotask_jobdelegate_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.macrotask
    ADD CONSTRAINT macrotask_jobdelegate_id_fkey FOREIGN KEY (jobdelegate_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4464 (class 2606 OID 17491)
-- Name: macrotask macrotask_macroactions_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.macrotask
    ADD CONSTRAINT macrotask_macroactions_id_fkey FOREIGN KEY (macroactions_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4465 (class 2606 OID 17496)
-- Name: macrotask macrotask_realm_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.macrotask
    ADD CONSTRAINT macrotask_realm_id_fkey FOREIGN KEY (realm_id) REFERENCES public.realm(id) DEFERRABLE;


--
-- TOC entry 4466 (class 2606 OID 17506)
-- Name: macrotaskcommand macrotaskcommand_command_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.macrotaskcommand
    ADD CONSTRAINT macrotaskcommand_command_id_fkey FOREIGN KEY (command_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4467 (class 2606 OID 17501)
-- Name: macrotaskcommand macrotaskcommand_macrotask_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.macrotaskcommand
    ADD CONSTRAINT macrotaskcommand_macrotask_id_fkey FOREIGN KEY (macrotask_id) REFERENCES public.macrotask(id) DEFERRABLE;


--
-- TOC entry 4468 (class 2606 OID 17511)
-- Name: macrotaskexec macrotaskexec_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.macrotaskexec
    ADD CONSTRAINT macrotaskexec_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.macrotask(id) DEFERRABLE;


--
-- TOC entry 4469 (class 2606 OID 17516)
-- Name: notification notification_recipientsprovider_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_recipientsprovider_id_fkey FOREIGN KEY (recipientsprovider_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4470 (class 2606 OID 17521)
-- Name: notification notification_template_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_template_id_fkey FOREIGN KEY (template_id) REFERENCES public.mailtemplate(id) DEFERRABLE;


--
-- TOC entry 4471 (class 2606 OID 17526)
-- Name: notificationtask notificationtask_notification_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificationtask
    ADD CONSTRAINT notificationtask_notification_id_fkey FOREIGN KEY (notification_id) REFERENCES public.notification(id) DEFERRABLE;


--
-- TOC entry 4472 (class 2606 OID 17531)
-- Name: notificationtaskexec notificationtaskexec_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificationtaskexec
    ADD CONSTRAINT notificationtaskexec_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.notificationtask(id) DEFERRABLE;


--
-- TOC entry 4473 (class 2606 OID 17541)
-- Name: oidcprovideraction oidcprovideraction_implementation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcprovideraction
    ADD CONSTRAINT oidcprovideraction_implementation_id_fkey FOREIGN KEY (implementation_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4474 (class 2606 OID 17536)
-- Name: oidcprovideraction oidcprovideraction_op_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcprovideraction
    ADD CONSTRAINT oidcprovideraction_op_id_fkey FOREIGN KEY (op_id) REFERENCES public.oidcprovider(id) DEFERRABLE;


--
-- TOC entry 4475 (class 2606 OID 17546)
-- Name: oidcrpclientapp oidcrpclientapp_accesspolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcrpclientapp
    ADD CONSTRAINT oidcrpclientapp_accesspolicy_id_fkey FOREIGN KEY (accesspolicy_id) REFERENCES public.accesspolicy(id) DEFERRABLE;


--
-- TOC entry 4476 (class 2606 OID 17551)
-- Name: oidcrpclientapp oidcrpclientapp_attrreleasepolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcrpclientapp
    ADD CONSTRAINT oidcrpclientapp_attrreleasepolicy_id_fkey FOREIGN KEY (attrreleasepolicy_id) REFERENCES public.attrreleasepolicy(id) DEFERRABLE;


--
-- TOC entry 4477 (class 2606 OID 17556)
-- Name: oidcrpclientapp oidcrpclientapp_authpolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcrpclientapp
    ADD CONSTRAINT oidcrpclientapp_authpolicy_id_fkey FOREIGN KEY (authpolicy_id) REFERENCES public.authpolicy(id) DEFERRABLE;


--
-- TOC entry 4478 (class 2606 OID 17561)
-- Name: oidcrpclientapp oidcrpclientapp_realm_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcrpclientapp
    ADD CONSTRAINT oidcrpclientapp_realm_id_fkey FOREIGN KEY (realm_id) REFERENCES public.realm(id) DEFERRABLE;


--
-- TOC entry 4479 (class 2606 OID 17566)
-- Name: oidcrpclientapp oidcrpclientapp_ticketexpirationpolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcrpclientapp
    ADD CONSTRAINT oidcrpclientapp_ticketexpirationpolicy_id_fkey FOREIGN KEY (ticketexpirationpolicy_id) REFERENCES public.ticketexpirationpolicy(id) DEFERRABLE;


--
-- TOC entry 4480 (class 2606 OID 17576)
-- Name: oidcusertemplate oidcusertemplate_anytype_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcusertemplate
    ADD CONSTRAINT oidcusertemplate_anytype_id_fkey FOREIGN KEY (anytype_id) REFERENCES public.anytype(id) DEFERRABLE;


--
-- TOC entry 4481 (class 2606 OID 17571)
-- Name: oidcusertemplate oidcusertemplate_op_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oidcusertemplate
    ADD CONSTRAINT oidcusertemplate_op_id_fkey FOREIGN KEY (op_id) REFERENCES public.oidcprovider(id) DEFERRABLE;


--
-- TOC entry 4482 (class 2606 OID 17586)
-- Name: passwordpolicyrule passwordpolicyrule_implementation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.passwordpolicyrule
    ADD CONSTRAINT passwordpolicyrule_implementation_id_fkey FOREIGN KEY (implementation_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4483 (class 2606 OID 17581)
-- Name: passwordpolicyrule passwordpolicyrule_policy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.passwordpolicyrule
    ADD CONSTRAINT passwordpolicyrule_policy_id_fkey FOREIGN KEY (policy_id) REFERENCES public.passwordpolicy(id) DEFERRABLE;


--
-- TOC entry 4484 (class 2606 OID 17596)
-- Name: plainschema plainschema_anytypeclass_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.plainschema
    ADD CONSTRAINT plainschema_anytypeclass_id_fkey FOREIGN KEY (anytypeclass_id) REFERENCES public.anytypeclass(id) DEFERRABLE;


--
-- TOC entry 4485 (class 2606 OID 17601)
-- Name: plainschema plainschema_dropdownvalueprovider_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.plainschema
    ADD CONSTRAINT plainschema_dropdownvalueprovider_id_fkey FOREIGN KEY (dropdownvalueprovider_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4486 (class 2606 OID 17591)
-- Name: plainschema plainschema_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.plainschema
    ADD CONSTRAINT plainschema_id_fkey FOREIGN KEY (id) REFERENCES public.syncopeschema(id) DEFERRABLE;


--
-- TOC entry 4487 (class 2606 OID 17606)
-- Name: plainschema plainschema_validator_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.plainschema
    ADD CONSTRAINT plainschema_validator_id_fkey FOREIGN KEY (validator_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4488 (class 2606 OID 17611)
-- Name: propagationtask propagationtask_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.propagationtask
    ADD CONSTRAINT propagationtask_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES public.externalresource(id) DEFERRABLE;


--
-- TOC entry 4489 (class 2606 OID 17616)
-- Name: propagationtaskexec propagationtaskexec_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.propagationtaskexec
    ADD CONSTRAINT propagationtaskexec_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.propagationtask(id) DEFERRABLE;


--
-- TOC entry 4490 (class 2606 OID 17631)
-- Name: pulltask pulltask_destinationrealm_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pulltask
    ADD CONSTRAINT pulltask_destinationrealm_id_fkey FOREIGN KEY (destinationrealm_id) REFERENCES public.realm(id) DEFERRABLE;


--
-- TOC entry 4491 (class 2606 OID 17621)
-- Name: pulltask pulltask_jobdelegate_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pulltask
    ADD CONSTRAINT pulltask_jobdelegate_id_fkey FOREIGN KEY (jobdelegate_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4492 (class 2606 OID 17636)
-- Name: pulltask pulltask_reconfilterbuilder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pulltask
    ADD CONSTRAINT pulltask_reconfilterbuilder_id_fkey FOREIGN KEY (reconfilterbuilder_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4493 (class 2606 OID 17626)
-- Name: pulltask pulltask_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pulltask
    ADD CONSTRAINT pulltask_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES public.externalresource(id) DEFERRABLE;


--
-- TOC entry 4494 (class 2606 OID 17646)
-- Name: pulltaskaction pulltaskaction_implementation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pulltaskaction
    ADD CONSTRAINT pulltaskaction_implementation_id_fkey FOREIGN KEY (implementation_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4495 (class 2606 OID 17641)
-- Name: pulltaskaction pulltaskaction_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pulltaskaction
    ADD CONSTRAINT pulltaskaction_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.pulltask(id) DEFERRABLE;


--
-- TOC entry 4496 (class 2606 OID 17651)
-- Name: pulltaskexec pulltaskexec_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pulltaskexec
    ADD CONSTRAINT pulltaskexec_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.pulltask(id) DEFERRABLE;


--
-- TOC entry 4497 (class 2606 OID 17661)
-- Name: pushcorrelationruleentity pushcorrelationruleentity_anytype_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pushcorrelationruleentity
    ADD CONSTRAINT pushcorrelationruleentity_anytype_id_fkey FOREIGN KEY (anytype_id) REFERENCES public.anytype(id) DEFERRABLE;


--
-- TOC entry 4498 (class 2606 OID 17666)
-- Name: pushcorrelationruleentity pushcorrelationruleentity_implementation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pushcorrelationruleentity
    ADD CONSTRAINT pushcorrelationruleentity_implementation_id_fkey FOREIGN KEY (implementation_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4499 (class 2606 OID 17656)
-- Name: pushcorrelationruleentity pushcorrelationruleentity_pushpolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pushcorrelationruleentity
    ADD CONSTRAINT pushcorrelationruleentity_pushpolicy_id_fkey FOREIGN KEY (pushpolicy_id) REFERENCES public.pushpolicy(id) DEFERRABLE;


--
-- TOC entry 4500 (class 2606 OID 17671)
-- Name: pushtask pushtask_jobdelegate_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pushtask
    ADD CONSTRAINT pushtask_jobdelegate_id_fkey FOREIGN KEY (jobdelegate_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4501 (class 2606 OID 17676)
-- Name: pushtask pushtask_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pushtask
    ADD CONSTRAINT pushtask_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES public.externalresource(id) DEFERRABLE;


--
-- TOC entry 4502 (class 2606 OID 17681)
-- Name: pushtask pushtask_sourcerealm_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pushtask
    ADD CONSTRAINT pushtask_sourcerealm_id_fkey FOREIGN KEY (sourcerealm_id) REFERENCES public.realm(id) DEFERRABLE;


--
-- TOC entry 4503 (class 2606 OID 17691)
-- Name: pushtaskaction pushtaskaction_implementation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pushtaskaction
    ADD CONSTRAINT pushtaskaction_implementation_id_fkey FOREIGN KEY (implementation_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4504 (class 2606 OID 17686)
-- Name: pushtaskaction pushtaskaction_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pushtaskaction
    ADD CONSTRAINT pushtaskaction_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.pushtask(id) DEFERRABLE;


--
-- TOC entry 4505 (class 2606 OID 17696)
-- Name: pushtaskexec pushtaskexec_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pushtaskexec
    ADD CONSTRAINT pushtaskexec_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.pushtask(id) DEFERRABLE;


--
-- TOC entry 4506 (class 2606 OID 17701)
-- Name: realm realm_accesspolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT realm_accesspolicy_id_fkey FOREIGN KEY (accesspolicy_id) REFERENCES public.accesspolicy(id) DEFERRABLE;


--
-- TOC entry 4507 (class 2606 OID 17706)
-- Name: realm realm_accountpolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT realm_accountpolicy_id_fkey FOREIGN KEY (accountpolicy_id) REFERENCES public.accountpolicy(id) DEFERRABLE;


--
-- TOC entry 4515 (class 2606 OID 17751)
-- Name: realm_anytypeclass realm_anytypeclass_anytypeclass_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_anytypeclass
    ADD CONSTRAINT realm_anytypeclass_anytypeclass_id_fkey FOREIGN KEY (anytypeclass_id) REFERENCES public.anytypeclass(id) DEFERRABLE;


--
-- TOC entry 4516 (class 2606 OID 17746)
-- Name: realm_anytypeclass realm_anytypeclass_realm_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_anytypeclass
    ADD CONSTRAINT realm_anytypeclass_realm_id_fkey FOREIGN KEY (realm_id) REFERENCES public.realm(id) DEFERRABLE;


--
-- TOC entry 4508 (class 2606 OID 17711)
-- Name: realm realm_attrreleasepolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT realm_attrreleasepolicy_id_fkey FOREIGN KEY (attrreleasepolicy_id) REFERENCES public.attrreleasepolicy(id) DEFERRABLE;


--
-- TOC entry 4509 (class 2606 OID 17716)
-- Name: realm realm_authpolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT realm_authpolicy_id_fkey FOREIGN KEY (authpolicy_id) REFERENCES public.authpolicy(id) DEFERRABLE;


--
-- TOC entry 4517 (class 2606 OID 17756)
-- Name: realm_externalresource realm_externalresource_realm_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_externalresource
    ADD CONSTRAINT realm_externalresource_realm_id_fkey FOREIGN KEY (realm_id) REFERENCES public.realm(id) DEFERRABLE;


--
-- TOC entry 4518 (class 2606 OID 17761)
-- Name: realm_externalresource realm_externalresource_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm_externalresource
    ADD CONSTRAINT realm_externalresource_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES public.externalresource(id) DEFERRABLE;


--
-- TOC entry 4510 (class 2606 OID 17721)
-- Name: realm realm_parent_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT realm_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.realm(id) DEFERRABLE;


--
-- TOC entry 4511 (class 2606 OID 17726)
-- Name: realm realm_passwordpolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT realm_passwordpolicy_id_fkey FOREIGN KEY (passwordpolicy_id) REFERENCES public.passwordpolicy(id) DEFERRABLE;


--
-- TOC entry 4512 (class 2606 OID 17731)
-- Name: realm realm_ticketexpirationpolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT realm_ticketexpirationpolicy_id_fkey FOREIGN KEY (ticketexpirationpolicy_id) REFERENCES public.ticketexpirationpolicy(id) DEFERRABLE;


--
-- TOC entry 4513 (class 2606 OID 17741)
-- Name: realmaction realmaction_implementation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realmaction
    ADD CONSTRAINT realmaction_implementation_id_fkey FOREIGN KEY (implementation_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4514 (class 2606 OID 17736)
-- Name: realmaction realmaction_realm_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.realmaction
    ADD CONSTRAINT realmaction_realm_id_fkey FOREIGN KEY (realm_id) REFERENCES public.realm(id) DEFERRABLE;


--
-- TOC entry 4519 (class 2606 OID 17766)
-- Name: relationshiptype relationshiptype_leftendanytype_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.relationshiptype
    ADD CONSTRAINT relationshiptype_leftendanytype_id_fkey FOREIGN KEY (leftendanytype_id) REFERENCES public.anytype(id) DEFERRABLE;


--
-- TOC entry 4520 (class 2606 OID 17771)
-- Name: relationshiptype relationshiptype_rightendanytype_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.relationshiptype
    ADD CONSTRAINT relationshiptype_rightendanytype_id_fkey FOREIGN KEY (rightendanytype_id) REFERENCES public.anytype(id) DEFERRABLE;


--
-- TOC entry 4521 (class 2606 OID 17776)
-- Name: remediation remediation_anytype_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.remediation
    ADD CONSTRAINT remediation_anytype_id_fkey FOREIGN KEY (anytype_id) REFERENCES public.anytype(id) DEFERRABLE;


--
-- TOC entry 4522 (class 2606 OID 17781)
-- Name: remediation remediation_pulltask_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.remediation
    ADD CONSTRAINT remediation_pulltask_id_fkey FOREIGN KEY (pulltask_id) REFERENCES public.pulltask(id) DEFERRABLE;


--
-- TOC entry 4523 (class 2606 OID 17786)
-- Name: report report_jobdelegate_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.report
    ADD CONSTRAINT report_jobdelegate_id_fkey FOREIGN KEY (jobdelegate_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4524 (class 2606 OID 17791)
-- Name: reportexec reportexec_report_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reportexec
    ADD CONSTRAINT reportexec_report_id_fkey FOREIGN KEY (report_id) REFERENCES public.report(id) DEFERRABLE;


--
-- TOC entry 4525 (class 2606 OID 17801)
-- Name: saml2idp4uiaction saml2idp4uiaction_implementation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2idp4uiaction
    ADD CONSTRAINT saml2idp4uiaction_implementation_id_fkey FOREIGN KEY (implementation_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4526 (class 2606 OID 17796)
-- Name: saml2idp4uiaction saml2idp4uiaction_saml2idp4ui_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2idp4uiaction
    ADD CONSTRAINT saml2idp4uiaction_saml2idp4ui_id_fkey FOREIGN KEY (saml2idp4ui_id) REFERENCES public.saml2sp4uiidp(id) DEFERRABLE;


--
-- TOC entry 4527 (class 2606 OID 17806)
-- Name: saml2sp4uiidp saml2sp4uiidp_requestedauthncontextprovider_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2sp4uiidp
    ADD CONSTRAINT saml2sp4uiidp_requestedauthncontextprovider_id_fkey FOREIGN KEY (requestedauthncontextprovider_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4528 (class 2606 OID 17816)
-- Name: saml2sp4uiusertemplate saml2sp4uiusertemplate_anytype_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2sp4uiusertemplate
    ADD CONSTRAINT saml2sp4uiusertemplate_anytype_id_fkey FOREIGN KEY (anytype_id) REFERENCES public.anytype(id) DEFERRABLE;


--
-- TOC entry 4529 (class 2606 OID 17811)
-- Name: saml2sp4uiusertemplate saml2sp4uiusertemplate_idp_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2sp4uiusertemplate
    ADD CONSTRAINT saml2sp4uiusertemplate_idp_id_fkey FOREIGN KEY (idp_id) REFERENCES public.saml2sp4uiidp(id) DEFERRABLE;


--
-- TOC entry 4530 (class 2606 OID 17821)
-- Name: saml2spclientapp saml2spclientapp_accesspolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2spclientapp
    ADD CONSTRAINT saml2spclientapp_accesspolicy_id_fkey FOREIGN KEY (accesspolicy_id) REFERENCES public.accesspolicy(id) DEFERRABLE;


--
-- TOC entry 4531 (class 2606 OID 17826)
-- Name: saml2spclientapp saml2spclientapp_attrreleasepolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2spclientapp
    ADD CONSTRAINT saml2spclientapp_attrreleasepolicy_id_fkey FOREIGN KEY (attrreleasepolicy_id) REFERENCES public.attrreleasepolicy(id) DEFERRABLE;


--
-- TOC entry 4532 (class 2606 OID 17831)
-- Name: saml2spclientapp saml2spclientapp_authpolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2spclientapp
    ADD CONSTRAINT saml2spclientapp_authpolicy_id_fkey FOREIGN KEY (authpolicy_id) REFERENCES public.authpolicy(id) DEFERRABLE;


--
-- TOC entry 4533 (class 2606 OID 17836)
-- Name: saml2spclientapp saml2spclientapp_realm_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2spclientapp
    ADD CONSTRAINT saml2spclientapp_realm_id_fkey FOREIGN KEY (realm_id) REFERENCES public.realm(id) DEFERRABLE;


--
-- TOC entry 4534 (class 2606 OID 17841)
-- Name: saml2spclientapp saml2spclientapp_ticketexpirationpolicy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saml2spclientapp
    ADD CONSTRAINT saml2spclientapp_ticketexpirationpolicy_id_fkey FOREIGN KEY (ticketexpirationpolicy_id) REFERENCES public.ticketexpirationpolicy(id) DEFERRABLE;


--
-- TOC entry 4535 (class 2606 OID 17846)
-- Name: schedtask schedtask_jobdelegate_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schedtask
    ADD CONSTRAINT schedtask_jobdelegate_id_fkey FOREIGN KEY (jobdelegate_id) REFERENCES public.implementation(id) DEFERRABLE;


--
-- TOC entry 4536 (class 2606 OID 17851)
-- Name: schedtaskexec schedtaskexec_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schedtaskexec
    ADD CONSTRAINT schedtaskexec_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.schedtask(id) DEFERRABLE;


--
-- TOC entry 4540 (class 2606 OID 17876)
-- Name: syncopegroup_anytypeclass syncopegroup_anytypeclass_anytypeclass_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopegroup_anytypeclass
    ADD CONSTRAINT syncopegroup_anytypeclass_anytypeclass_id_fkey FOREIGN KEY (anytypeclass_id) REFERENCES public.anytypeclass(id) DEFERRABLE;


--
-- TOC entry 4541 (class 2606 OID 17871)
-- Name: syncopegroup_anytypeclass syncopegroup_anytypeclass_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopegroup_anytypeclass
    ADD CONSTRAINT syncopegroup_anytypeclass_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.syncopegroup(id) DEFERRABLE;


--
-- TOC entry 4542 (class 2606 OID 17881)
-- Name: syncopegroup_externalresource syncopegroup_externalresource_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopegroup_externalresource
    ADD CONSTRAINT syncopegroup_externalresource_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.syncopegroup(id) DEFERRABLE;


--
-- TOC entry 4543 (class 2606 OID 17886)
-- Name: syncopegroup_externalresource syncopegroup_externalresource_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopegroup_externalresource
    ADD CONSTRAINT syncopegroup_externalresource_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES public.externalresource(id) DEFERRABLE;


--
-- TOC entry 4537 (class 2606 OID 17861)
-- Name: syncopegroup syncopegroup_groupowner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopegroup
    ADD CONSTRAINT syncopegroup_groupowner_id_fkey FOREIGN KEY (groupowner_id) REFERENCES public.syncopegroup(id) DEFERRABLE;


--
-- TOC entry 4538 (class 2606 OID 17856)
-- Name: syncopegroup syncopegroup_realm_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopegroup
    ADD CONSTRAINT syncopegroup_realm_id_fkey FOREIGN KEY (realm_id) REFERENCES public.realm(id) DEFERRABLE;


--
-- TOC entry 4539 (class 2606 OID 17866)
-- Name: syncopegroup syncopegroup_userowner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopegroup
    ADD CONSTRAINT syncopegroup_userowner_id_fkey FOREIGN KEY (userowner_id) REFERENCES public.syncopeuser(id) DEFERRABLE;


--
-- TOC entry 4544 (class 2606 OID 17896)
-- Name: syncoperole_dynrealm syncoperole_dynrealm_dynamicrealm_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncoperole_dynrealm
    ADD CONSTRAINT syncoperole_dynrealm_dynamicrealm_id_fkey FOREIGN KEY (dynamicrealm_id) REFERENCES public.dynrealm(id) DEFERRABLE;


--
-- TOC entry 4545 (class 2606 OID 17891)
-- Name: syncoperole_dynrealm syncoperole_dynrealm_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncoperole_dynrealm
    ADD CONSTRAINT syncoperole_dynrealm_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.syncoperole(id) DEFERRABLE;


--
-- TOC entry 4546 (class 2606 OID 17906)
-- Name: syncoperole_realm syncoperole_realm_realm_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncoperole_realm
    ADD CONSTRAINT syncoperole_realm_realm_id_fkey FOREIGN KEY (realm_id) REFERENCES public.realm(id) DEFERRABLE;


--
-- TOC entry 4547 (class 2606 OID 17901)
-- Name: syncoperole_realm syncoperole_realm_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncoperole_realm
    ADD CONSTRAINT syncoperole_realm_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.syncoperole(id) DEFERRABLE;


--
-- TOC entry 4550 (class 2606 OID 17926)
-- Name: syncopeuser_anytypeclass syncopeuser_anytypeclass_anytypeclass_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopeuser_anytypeclass
    ADD CONSTRAINT syncopeuser_anytypeclass_anytypeclass_id_fkey FOREIGN KEY (anytypeclass_id) REFERENCES public.anytypeclass(id) DEFERRABLE;


--
-- TOC entry 4551 (class 2606 OID 17921)
-- Name: syncopeuser_anytypeclass syncopeuser_anytypeclass_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopeuser_anytypeclass
    ADD CONSTRAINT syncopeuser_anytypeclass_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.syncopeuser(id) DEFERRABLE;


--
-- TOC entry 4552 (class 2606 OID 17936)
-- Name: syncopeuser_externalresource syncopeuser_externalresource_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopeuser_externalresource
    ADD CONSTRAINT syncopeuser_externalresource_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES public.externalresource(id) DEFERRABLE;


--
-- TOC entry 4553 (class 2606 OID 17931)
-- Name: syncopeuser_externalresource syncopeuser_externalresource_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopeuser_externalresource
    ADD CONSTRAINT syncopeuser_externalresource_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.syncopeuser(id) DEFERRABLE;


--
-- TOC entry 4548 (class 2606 OID 17911)
-- Name: syncopeuser syncopeuser_realm_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopeuser
    ADD CONSTRAINT syncopeuser_realm_id_fkey FOREIGN KEY (realm_id) REFERENCES public.realm(id) DEFERRABLE;


--
-- TOC entry 4549 (class 2606 OID 17916)
-- Name: syncopeuser syncopeuser_securityquestion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopeuser
    ADD CONSTRAINT syncopeuser_securityquestion_id_fkey FOREIGN KEY (securityquestion_id) REFERENCES public.securityquestion(id) DEFERRABLE;


--
-- TOC entry 4554 (class 2606 OID 17946)
-- Name: syncopeuser_syncoperole syncopeuser_syncoperole_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopeuser_syncoperole
    ADD CONSTRAINT syncopeuser_syncoperole_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.syncoperole(id) DEFERRABLE;


--
-- TOC entry 4555 (class 2606 OID 17941)
-- Name: syncopeuser_syncoperole syncopeuser_syncoperole_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.syncopeuser_syncoperole
    ADD CONSTRAINT syncopeuser_syncoperole_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.syncopeuser(id) DEFERRABLE;


--
-- TOC entry 4556 (class 2606 OID 17956)
-- Name: typeextension typeextension_anytype_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.typeextension
    ADD CONSTRAINT typeextension_anytype_id_fkey FOREIGN KEY (anytype_id) REFERENCES public.anytype(id) DEFERRABLE;


--
-- TOC entry 4558 (class 2606 OID 17966)
-- Name: typeextension_anytypeclass typeextension_anytypeclass_anytypeclass_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.typeextension_anytypeclass
    ADD CONSTRAINT typeextension_anytypeclass_anytypeclass_id_fkey FOREIGN KEY (anytypeclass_id) REFERENCES public.anytypeclass(id) DEFERRABLE;


--
-- TOC entry 4559 (class 2606 OID 17961)
-- Name: typeextension_anytypeclass typeextension_anytypeclass_typeextension_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.typeextension_anytypeclass
    ADD CONSTRAINT typeextension_anytypeclass_typeextension_id_fkey FOREIGN KEY (typeextension_id) REFERENCES public.typeextension(id) DEFERRABLE;


--
-- TOC entry 4557 (class 2606 OID 17951)
-- Name: typeextension typeextension_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.typeextension
    ADD CONSTRAINT typeextension_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.syncopegroup(id) DEFERRABLE;


--
-- TOC entry 4560 (class 2606 OID 17971)
-- Name: udyngroupmembership udyngroupmembership_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.udyngroupmembership
    ADD CONSTRAINT udyngroupmembership_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.syncopegroup(id) DEFERRABLE;


--
-- TOC entry 4561 (class 2606 OID 17981)
-- Name: umembership umembership_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.umembership
    ADD CONSTRAINT umembership_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.syncopegroup(id) DEFERRABLE;


--
-- TOC entry 4562 (class 2606 OID 17976)
-- Name: umembership umembership_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.umembership
    ADD CONSTRAINT umembership_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.syncopeuser(id) DEFERRABLE;


--
-- TOC entry 4563 (class 2606 OID 17991)
-- Name: urelationship urelationship_anyobject_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.urelationship
    ADD CONSTRAINT urelationship_anyobject_id_fkey FOREIGN KEY (anyobject_id) REFERENCES public.anyobject(id) DEFERRABLE;


--
-- TOC entry 4564 (class 2606 OID 17996)
-- Name: urelationship urelationship_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.urelationship
    ADD CONSTRAINT urelationship_type_id_fkey FOREIGN KEY (type_id) REFERENCES public.relationshiptype(id) DEFERRABLE;


--
-- TOC entry 4565 (class 2606 OID 17986)
-- Name: urelationship urelationship_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.urelationship
    ADD CONSTRAINT urelationship_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.syncopeuser(id) DEFERRABLE;
