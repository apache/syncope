package org.syncope.console;

/**
 * SyncopeUser to store in SyncopeSession after the authentication.
 * @author lbrandolini
 */
public class SyncopeUser implements java.io.Serializable
{

    private String username;
    private String name;
    private String surname;

    private int role;
    private String email;

    public String getUsername()
    {
        return username;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    public int getRole()
    {
        return role;
    }

    public void setRole( int role )
    {
        this.role = role;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getSurname()
    {
        return surname;
    }

    public void setSurname( String surname )
    {
        this.surname = surname;
    }
}
