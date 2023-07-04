# User site

A user-site forms a link between a user and a site.
The existence of a user-site implies that, at some point, the user has added the site to their 'list of sites'.


## Lifecycle

A user-site is created by the system once a [user](user.md) has given consent to a financial institution.
Before the user has given consent, but after the user has first indicated that it wants to add a financial institution we work with a [user-site-session](user-site-session.md).
See [function: add a site](../functions/add-site.md) for more information on this process.


## Related entities

Transaction data in the form of **accounts** and **transactions** all link back to a **user-site**.


## Properties

**status**

The status field is used to keep track of what status a user-site is in.
The default state is `INITIAL_PROCESSING`, this means the site is 'ready' to be refreshed and no processes are currently running for this user-site.
For the state transition diagrams, please see [user-site-status](user-site-status.md)


**persistedFormStepAnswers**

During the [add bank flow](../functions/add-site.md) a [user](user.md) must sometimes fill in a [form step](step.md).
A form step consist of one or more FormFields that the user must fill in.
A provider module can set the `persist` flag of a FormField to true because some of the answers might be re-usable on subsequent re-consent / renew-accessmeans flows.
This column keeps track of answers given **only if** FormField.persist is set to true for any given field in the form.
site-management will remember the answer that the user gave to the question represented by the FormField when the user posts the form back to site-management.

site-management will auto-complete a subsequent form (during a re-consent flow) automatically if previously given answers are sufficient to complete a requested FormStep with.

**Limitation**
Every time a user posts a form the value is overwritten and previous answers will be lost.
This means that if a provider has an add bank flow with more than 1 FormStep this feature is not very helpful.
So long as there is only a single FormStep needed for an add-bank/renew-access flow this feature will enable providers to for example pre-fill form values.
