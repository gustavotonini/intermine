package org.intermine.webservice.server.output;

/*
* Copyright (C) 2002-2011 FlyMine
*
* This code may be freely distributed and modified under the
* terms of the GNU Lesser General Public Licence.  This should
* be distributed with the code.  See the LICENSE file for more
* information or http://www.gnu.org/copyleft/lesser.html.
*
*/


import java.util.Iterator;

import org.intermine.api.InterMineAPI;
import org.intermine.api.results.ExportResultsIterator;

public class JSONRowResultProcessor extends JSONResultProcessor
{
    private final InterMineAPI im;
    /**
     * Constructor.
     * @param baseUrl The base URL to be used for constructing links with.
     */
    public JSONRowResultProcessor(InterMineAPI im) {
        this.im = im;
    }

    @Override
    protected Iterator<? extends Object> getResultsIterator(ExportResultsIterator it) {
        JSONRowIterator jsonIter = new JSONRowIterator(it, im);
        return jsonIter;
    }

}
