# User site session

A user site session exists to keep track of state that is accumulated during flows, for instance: [the add site flow](../functions/add-site.md).
There *cannot* exist concurrent user site sessions for the same [user site](user-site.md).
There *can* exist more than one user site session for the same [user](user.md).



## Lifecycle

A user site session is created because a user performs an action.
It is *never* created by the system as part of an automated process (for example the flywheel).

These are all the situations in which the system creates a user site session:

- the user adds a bank, see [add site flow](../functions/add-site.md)
- the user renews access for a bank, see [renew consent flow](../functions/renew-consent.md)

**todo** is the above list exhaustive?


## Properties

**usesDynamicSteps** `boolean`

There exist two sets of endpoints that interact with user-site-sessions.
This field exists to determine of which 'flow' this user-site-session is part.
In other words: this boolean is a piece of legacy that can go once clients stop using the 'old endpoints'.

More about the distinction between 'old' and 'new' endpoints can be found in the concept [flow version](flow-version.md).
