import { Navbar, Container, Nav } from "react-bootstrap";

function AdminNavbar() {
  return (
    <Navbar bg="dark" variant="dark">
      <Container fluid>
        <Nav className="me-auto">
          <Nav.Item>
            <Nav.Link href="#home" onClick={(e) => e.preventDefault()}>
              Kry Service Monitoring Dashboard
            </Nav.Link>
          </Nav.Item>
        </Nav>
      </Container>
    </Navbar>
  );
}

export default AdminNavbar;
