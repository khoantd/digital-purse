-- Compensating reverse: type catalog + link from reverse tx to original.
INSERT INTO public."type" (id, "name", description)
VALUES (4, 'Reverse', 'Compensating reverse of a prior money movement');

SELECT setval('type_seq', (SELECT MAX(id) FROM public."type"));

ALTER TABLE public.transaction
    ADD COLUMN reverses_transaction_id BIGINT NULL;

ALTER TABLE public.transaction
    ADD CONSTRAINT fk_transaction_reverses
        FOREIGN KEY (reverses_transaction_id) REFERENCES public.transaction (id);

CREATE UNIQUE INDEX uq_transaction_reverses_transaction_id
    ON public.transaction (reverses_transaction_id)
    WHERE reverses_transaction_id IS NOT NULL;

ALTER TABLE public.spend_request
    ADD COLUMN source_transaction_id BIGINT NULL;

ALTER TABLE public.spend_request
    ADD CONSTRAINT fk_spend_request_source_transaction
        FOREIGN KEY (source_transaction_id) REFERENCES public.transaction (id);
