/**
 * 
 */
package com.jbent.peoplecentral.permission;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.acls.domain.DefaultPermissionFactory;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.PermissionFactory;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.taglibs.authz.AccessControlListTag;
import org.springframework.web.context.support.WebApplicationContextUtils;


public class SpringSecurityAccessControlListTag extends TagSupport {
    /**
	 * 
	 */
	private static final long serialVersionUID = 6951249085546551903L;

	//~ Static fields/initializers =====================================================================================

    protected static final Log logger = LogFactory.getLog(AccessControlListTag.class);

    //~ Instance fields ================================================================================================

    private AclService aclService;
    private ApplicationContext applicationContext;
    private Object domainObject;
    private ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy;
    private SidRetrievalStrategy sidRetrievalStrategy;
    private PermissionFactory permissionFactory;
    private String hasPermission = "";

    //~ Methods ========================================================================================================

    public int doStartTag() throws JspException {
        if ((null == hasPermission) || "".equals(hasPermission)) {
            return Tag.SKIP_BODY;
        }
        
        // By default Administrator has All permissions.ALWAYS Administrator is TRUE.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	    List<GrantedAuthority> authorities = (List<GrantedAuthority>) authentication.getAuthorities();
	    for(GrantedAuthority auth : authorities){
	    	if(auth.getAuthority().equalsIgnoreCase("ROLE_ADMINISTRATOR"))
	        	return Tag.EVAL_BODY_INCLUDE;
	        }


        initializeIfRequired();

        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext evalContext = new StandardEvaluationContext();
        evalContext.setVariable("hasPermission", getHasPermission());
        Expression exp = parser.parseExpression("#hasPermission");
        final String evaledPermissionsString  = exp.getValue(evalContext, String.class);
        /*Expression exp = parser.parseExpression("hasPermission");
        final String evaledPermissionsString  = exp.getValue(pageContext, String.class);*/

        List<Permission> requiredPermissions = parsePermissionsString(evaledPermissionsString);

        Object resolvedDomainObject = null;

        if (domainObject instanceof String) {
        	ExpressionParser parser1 = new SpelExpressionParser();
            Expression exp1 = parser1.parseExpression("domainObject");
            resolvedDomainObject=  exp1.getValue(pageContext, String.class);
        } else {
            resolvedDomainObject = domainObject;
        }

        if (resolvedDomainObject == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("domainObject resolved to null, so including tag body");
            }

            // Of course they have access to a null object!
            return Tag.EVAL_BODY_INCLUDE;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "SecurityContextHolder did not return a non-null Authentication object, so skipping tag body");
            }

            return Tag.SKIP_BODY;
        }

        List<Sid> sids = sidRetrievalStrategy.getSids(SecurityContextHolder.getContext().getAuthentication());
        ObjectIdentity oid = objectIdentityRetrievalStrategy.getObjectIdentity(resolvedDomainObject);

        // Obtain aclEntrys applying to the current Authentication object
        try {
            Acl acl = aclService.readAclById(oid, sids);

            if (acl.isGranted(requiredPermissions, sids, false)) {
                return Tag.EVAL_BODY_INCLUDE;
            } else {
                return Tag.SKIP_BODY;
            }
        } catch (NotFoundException nfe) {
            return Tag.SKIP_BODY;
        }
    }

    /**
     * Allows test cases to override where application context obtained from.
     *
     * @param pageContext so the <code>ServletContext</code> can be accessed as required by Spring's
     *        <code>WebApplicationContextUtils</code>
     *
     * @return the Spring application context (never <code>null</code>)
     */
    protected ApplicationContext getContext(PageContext pageContext) {
        ServletContext servletContext = pageContext.getServletContext();

        return WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
    }

    public Object getDomainObject() {
        return domainObject;
    }

    public String getHasPermission() {
        return hasPermission;
    }

    private void initializeIfRequired() throws JspException {
        if (applicationContext != null) {
            return;
        }

        this.applicationContext = getContext(pageContext);

        aclService = getBeanOfType(AclService.class);

        sidRetrievalStrategy = getBeanOfType(SidRetrievalStrategy.class);

        if (sidRetrievalStrategy == null) {
            sidRetrievalStrategy = new SidRetrievalStrategyImpl();
        }

        objectIdentityRetrievalStrategy = getBeanOfType(ObjectIdentityRetrievalStrategy.class);

        if (objectIdentityRetrievalStrategy == null) {
            objectIdentityRetrievalStrategy = new ObjectIdentityRetrievalStrategyImpl();
        }

        permissionFactory = getBeanOfType(PermissionFactory.class);

        if (permissionFactory == null) {
            permissionFactory = new DefaultPermissionFactory();
        }
    }

    private <T> T getBeanOfType(Class<T> type) throws JspException {
        Map<String, T> map = applicationContext.getBeansOfType(type);

        for (ApplicationContext context = applicationContext.getParent();
            context != null; context = context.getParent()) {
            map.putAll(context.getBeansOfType(type));
        }

        if (map.size() == 0) {
            return null;
        } else if (map.size() == 1) {
            return map.values().iterator().next();
        }

        throw new JspException("Found incorrect number of " + type.getSimpleName() +" instances in "
                    + "application context - you must have only have one!");
    }

    private List<Permission> parsePermissionsString(String permissionsString) throws NumberFormatException {
        final Set<Permission> permissions = new HashSet<Permission>();
        final StringTokenizer tokenizer;
        tokenizer = new StringTokenizer(permissionsString, ",", false);

        while (tokenizer.hasMoreTokens()) {
            String permission = tokenizer.nextToken();
            try {
                permissions.add(permissionFactory.buildFromMask(Integer.valueOf(permission)));
            } catch (NumberFormatException nfe) {
                // Not an integer mask. Try using a name
                permissions.add(permissionFactory.buildFromName(permission));
            }
        }

        return new ArrayList<Permission>(permissions);
    }

    public void setDomainObject(Object domainObject) {
        this.domainObject = domainObject;
    }

    public void setHasPermission(String hasPermission) {
        this.hasPermission = hasPermission;
    }
}
