package org.intermine.webservice.server.query;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.web.context.InterMineContext;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.NoServiceException;
import org.intermine.webservice.server.core.WebServiceServlet;

/**
 * Runs the query Upload Service to handle user query uploads.
 * @author Alexis Kalderimis
 *
 */
public class QueryUploadServlet extends WebServiceServlet
{

    @Override
    protected WebService getService(Method method) throws NoServiceException {
        switch (method) {
        case POST:
            return new QueryUploadService(api);
        }
        throw new NoServiceException();
    }

}
