# Sites

The sites list is now maintained in providers. We fetch the list synchronously from providers.


## Adding a site


## History

Prior to June 2020 all these sites were stored and maintained in Cassandra.
This setup made it difficult to see what sites exist and if everything has been configured correctly.


## Provider: YODLEE

Yodlee has been marked as @Deprecated because we have deactivated and removed the provider.
This means: users can no longer add user-sites at Yodlee or get new transaction data at Yodlee.
There are quite a few sites linked to the Yodlee provider that have 0 or very few user-sites.

Yodlee was only ever used on app-prd (and never on yfb-prd, yfb-sandbox, or yfb-ext-prd).
The below numbers were checked on 2020-06-04, it is important to note that they cannot have increased since that date since Yodlee is offline.
These are the Yodlee sites with 0 users:

```
YODLEE		ASN Bank		1cc275ce-787f-45e1-bc0a-f8746cf5731b has 0 user-sites.
YODLEE		Bankia (Autónomos) (ES)		261f3815-5de3-40d1-8592-a9a451632e86 has 0 user-sites.
YODLEE		Bankia (Autónomos) (ES)		96b05b00-dfa0-4bd9-a874-6bfb69ac8725 has 0 user-sites.
YODLEE		BBVA (Autónomos) (ES)		9ecad2e6-bae3-4154-90c2-9bf850119f9b has 0 user-sites.
YODLEE		BBVA (Autónomos) (ES)		ecbe2340-10d6-4053-8908-9307f710302d has 0 user-sites.
YODLEE		La Caixa (Autónomos) (ES)		8484ef46-d300-4ea9-8cd9-030db5bc6ab5 has 0 user-sites.
YODLEE		La Caixa (Autónomos) (ES)		0bf1223d-007c-42eb-a487-ed729e6fcaf5 has 0 user-sites.
YODLEE		Clydesdale Bank		7674411a-9fd9-4b9a-9949-356f46b463c7 has 0 user-sites.
YODLEE		ING (NL)		44bbc1b2-029e-11e9-8eb2-f2801f1b9fd1 has 0 user-sites.
YODLEE		Sainsbury's Bank		b80bef2b-66b7-4e6c-886d-f56c73597505 has 0 user-sites.
YODLEE		Santander (Autónomos) (ES)		de2e7fce-c247-4032-ae12-5211eb5efdf7 has 0 user-sites.
YODLEE		Santander (Autónomos) (ES)		d9785a7f-b45a-4d39-bf4e-ae131380cd20 has 0 user-sites.
YODLEE		SNS Bank		a6ccadc4-a2fa-11e9-a2a3-2a2ae2dbcce4 has 0 user-sites.
YODLEE		Tesco Bank		ea54690c-8353-4bbe-b9ad-bd4f72ecdcf8 has 0 user-sites.
YODLEE		TSB		b2d485f5-24d1-4990-860c-f18032be3e9a has 0 user-sites.
YODLEE		Knab Bank		455ae74c-562e-4033-ab33-9efc7d855998 has 0 user-sites. - Removed during Knab URL provider implementation
YODLEE		Virgin Money Savings		8d27bff5-9855-46d9-97c5-264699ab663f has 0 user-sites.
YODLEE		Yorkshire Bank		4b16d319-080b-447d-a968-7f68f2aa5f9b has 0 user-sites.
```

These are the Yodlee sites with very few users.
The assumption is that these are all Yolt (ex-)employees and we can therefore do away with the site completely.
```
YODLEE		ABN AMRO		c7987867-3219-4396-8d56-d79aff85a073 has 13 user-sites.
YODLEE		ABN AMRO		bfdc30e3-1a08-4f2a-a85d-ac32c7227ccc has 2 user-sites.
YODLEE		ASN Bank		03433a5c-d1b1-41f7-b38f-119227bf7450 has 1 user-sites.
YODLEE		Rabobank		b17f3413-b84f-4495-8d5b-9ff1e840b7a6 has 5 user-sites.
YODLEE		Rabobank		b02fca30-65f6-470f-af1b-fbd5704abd56 has 1 user-sites.
YODLEE		SNS Bank		2a609329-e2e8-44ac-9ee4-8896d68625ce has 2 user-sites.
YODLEE		ANZ		51609c35-f962-42a4-922a-70565c4b7d6f has 2 user-sites.
YODLEE		Commonwealth Bank		d539fcd2-0c33-47bb-9dd9-410047713c9f has 2 user-sites.
YODLEE		ERSTE Bank (AT)		1c2a6867-e30d-4a04-8f45-11470fa1c5f0 has 1 user-sites.
YODLEE		ING DiBa (AT)		2aedd5c6-a8a6-4219-aeef-13f3d3b25881 has 1 user-sites.
YODLEE		ING DiBA (DE)		6f572529-ad42-4392-a3ad-fc256a8836d9 has 2 user-sites.
YODLEE		ING Direct (AUS)		442d9a21-58df-48b8-9b78-ae7140e27b55 has 6 user-sites.
YODLEE		ING Direct (ES)		23137d6a-f528-4ed7-8a34-53a470d19341 has 4 user-sites.
YODLEE		ING Direct (FR)		ecfc4d06-006e-4947-b036-aa4f2d75f067 has 5 user-sites.
YODLEE		Moneyou		dfb3c0ec-72b0-486b-ba98-9727cefbddbd has 2 user-sites.
YODLEE		NAB		fcf0bcdd-9ef7-4ca3-acea-a4646b496de3 has 1 user-sites.
YODLEE		PayPal		f5d8b10e-ce8d-47c0-b891-98f29ccc50ae has 10 user-sites.
YODLEE		Raiffeisen (AT)		95843cd4-7539-4d24-8dba-aca5eefe4532 has 2 user-sites.
YODLEE		Volksbank Wien (AT)		f782e8ef-6e79-4db9-b200-8acb550df2ea has 1 user-sites.
YODLEE		Westpac		b1067aaf-0733-4230-b875-a237b85756ff has 1 user-sites.
```