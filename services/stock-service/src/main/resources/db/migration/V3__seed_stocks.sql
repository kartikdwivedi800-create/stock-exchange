-- NX-206: seed the 20 tradable stocks.
-- NOTE: prices below are ILLUSTRATIVE PLACEHOLDERS, not real market data.
-- They will be replaced by the Kaggle seed pipeline in NX-608 (ADR-0011).
-- last_price, previous_close, day_open, day_high and day_low all start at the same price.

INSERT INTO stocks (symbol, company_name, sector, tick_size, lot_size,
                    last_price, previous_close, day_open, day_high, day_low,
                    created_at, updated_at)
SELECT s.symbol, s.company_name, s.sector, 0.05, 1,
       s.price, s.price, s.price, s.price, s.price,
       NOW(), NOW()
FROM (VALUES
    ('RELIANCE',   'Reliance Industries',           'Energy',             1400.0000),
    ('TCS',        'Tata Consultancy Services',     'IT',                 3500.0000),
    ('HDFCBANK',   'HDFC Bank',                     'Banking',            1700.0000),
    ('ICICIBANK',  'ICICI Bank',                    'Banking',            1400.0000),
    ('INFY',       'Infosys',                       'IT',                 1500.0000),
    ('BHARTIARTL', 'Bharti Airtel',                 'Telecom',            1900.0000),
    ('ITC',        'ITC Limited',                   'FMCG',                420.0000),
    ('SBIN',       'State Bank of India',           'Banking',             800.0000),
    ('LT',         'Larsen & Toubro',               'Infrastructure',     3600.0000),
    ('HINDUNILVR', 'Hindustan Unilever',            'FMCG',               2400.0000),
    ('KOTAKBANK',  'Kotak Mahindra Bank',           'Banking',            2000.0000),
    ('AXISBANK',   'Axis Bank',                     'Banking',            1150.0000),
    ('BAJFINANCE', 'Bajaj Finance',                 'Financial Services',  900.0000),
    ('MARUTI',     'Maruti Suzuki India',           'Automobile',        12500.0000),
    ('SUNPHARMA',  'Sun Pharmaceutical Industries', 'Pharma',             1700.0000),
    ('TATAMOTORS', 'Tata Motors',                   'Automobile',          700.0000),
    ('WIPRO',      'Wipro',                         'IT',                  250.0000),
    ('ASIANPAINT', 'Asian Paints',                  'Consumer',           2400.0000),
    ('HCLTECH',    'HCL Technologies',              'IT',                 1500.0000),
    ('TITAN',      'Titan Company',                 'Consumer',           3400.0000)
) AS s (symbol, company_name, sector, price);
