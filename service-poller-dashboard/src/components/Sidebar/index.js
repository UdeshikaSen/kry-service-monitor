
import { Nav } from "react-bootstrap";

function Sidebar() {
    return (
        <div className="sidebar">
            <div className="sidebar-wrapper">
                <div className="logo d-flex align-items-center justify-content-start">
                    <a className="simple-text" href="http://www.creative-tim.com">
                        Kry Metrics
                    </a>
                </div>
                <Nav>
                    <li>
                        <i className="nc-icon nc-alien-33" />
                        <p>Service Statuses</p>
                    </li>
                </Nav>
            </div>
        </div>
    );
}

export default Sidebar;