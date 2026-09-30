-- Align type catalog with money operations used by the app.
UPDATE public."type"
SET name = 'Withdraw',
    description = 'Withdraw funds'
WHERE id = 2;

UPDATE public."type"
SET name = 'Top-up',
    description = 'Add funds / top-up'
WHERE id = 3;

-- Reclassify existing transactions from ledger rail legs.
UPDATE public.transaction t
SET type_id = 2
WHERE EXISTS (
    SELECT 1
    FROM public.ledger_entry e
    WHERE e.transaction_id = t.id
      AND e.account_code = 'SYSTEM_FLOAT'
      AND e.entry_type = 'CREDIT'
);

UPDATE public.transaction t
SET type_id = 3
WHERE EXISTS (
    SELECT 1
    FROM public.ledger_entry e
    WHERE e.transaction_id = t.id
      AND e.account_code = 'SYSTEM_FLOAT'
      AND e.entry_type = 'DEBIT'
);
