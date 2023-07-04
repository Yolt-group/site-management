package nl.ing.lovebird.sitemanagement.usersite;

public enum UserSiteNeededAction {

    /**
     * User sees a button 'Log in to [bank]' when viewing accounts.
     */
    LOGIN_AGAIN("The user should login again with the used bank."),

    /**
     * User sees a button 'Update credentials' when viewing accounts.
     */
    UPDATE_CREDENTIALS("The user should update the credentials."),

    /**
     * User sees a button '<not implemented yet>' when viewing accounts.
     */
    UPDATE_WITH_NEXT_STEP("The user should complete the next step in order to complete the process."),

    /**
     * User sees a button 'Update credentials' when viewing accounts.
     */
    UPDATE_QUESTIONS("The user should submit MFA questions."),

    /**
     * User sees a button 'Refresh' when viewing accounts.
     */
    TRIGGER_REFRESH("The user should retry later."),

    DELETE_USER_SITE("The user should delete the user-site.");

    final String description;

    UserSiteNeededAction(final String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return String.format("%s : %s", name(), description);
    }

}
