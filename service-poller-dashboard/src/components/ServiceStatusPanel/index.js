import {
  Container,
  Row,
  Col,
  Table,
  Button,
  Modal,
  Form,
  Alert,
} from "react-bootstrap";
import React from "react";
import EventBus from "@vertx/eventbus-bridge-client.js";
import validate from "validate.js";
import {
  createService,
  getAllServices,
  updateService,
  deleteService,
} from "../../services/PollerService";

class ServiceStatusPanel extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      services: [],
      showServiceAddModal: false,
      showServiceUpdateModal: false,
      showServiceDeleteModal: false,

      addModalServiceName: "",
      addModalServiceURL: "",
      addModalError: "",

      updateModalServiceID: "",
      updateModalServiceName: "",
      updateModalServiceURL: "",
      updateModalError: "",

      deleteModalServiceID: "",
      deleteModalServiceName: "",
    };

    this.onServiceAdd = this.onServiceAdd.bind(this);
    this.onServiceUpdate = this.onServiceUpdate.bind(this);
    this.onServiceDelete = this.onServiceDelete.bind(this);
    this.updateServiceList = this.updateServiceList.bind(this);
    this.toggleServiceAddModalView = this.toggleServiceAddModalView.bind(this);
    this.toggleServiceUpdateModalView =
      this.toggleServiceUpdateModalView.bind(this);
    this.toggleServiceDeleteModalView =
      this.toggleServiceDeleteModalView.bind(this);
  }

  componentDidMount() {
    // initially display the services registered in the system
    this.updateServiceList();

    const options = {
      vertxbus_reconnect_attempts_max: Infinity,
      vertxbus_reconnect_delay_min: 1000,
      vertxbus_reconnect_delay_max: 3000,
      vertxbus_reconnect_exponent: 2,
      vertxbus_randomization_factor: 0.5,
    };
    //  call to the backend eventbus endpoint
    const eventBus = new EventBus("http://localhost:8090/eventbus", options);
    eventBus.enableReconnect(true);
    // open communication with the monitoring server to periodically sync service statuses
    eventBus.onopen = () => {
      // register an event bus handler to receive messages from backend sent to the address "kry.services"
      eventBus.registerHandler("kry.services", (error, message) => {
        console.log(message);
        const serviceWithStatus = JSON.parse(message.body);
        const services = this.state.services;
        services.forEach((svc) => {
          if (svc.id === serviceWithStatus.id) {
            svc.currentStatus = serviceWithStatus.currentStatus;
          }
        });

        this.setState({
          services,
        });
      });
    };

    eventBus.onreconnect = () => {
      this.updateServiceList();
    };
  }

  // get all registered services from backend
  updateServiceList() {
    getAllServices().then((res) => {
      console.log(res.data);
      this.setState({
        services: res.data.services,
      });
    });
  }

  // add service button click handler
  onServiceAdd() {
    // input validation
    if (validate.isEmpty(this.state.addModalServiceName)) {
      this.setState({
        addModalError: "Service name cannot be empty",
      });
      return;
    }
    if (
      validate.single(this.state.addModalServiceURL, {
        url: {
          allowLocal: true,
        },
      })
    ) {
      this.setState({
        addModalError: "Service URL is invalid",
      });
      return;
    }

    const newService = {
      name: this.state.addModalServiceName,
      url: this.state.addModalServiceURL,
    };

    createService(newService)
      .then((res) => {
        console.log(res.data);
        this.toggleServiceAddModalView(false);
        this.updateServiceList();
      })
      .catch((error) => {
        console.log("Error", error.message);
      });
  }

  // update service button click handler
  onServiceUpdate() {
    // input validation
    if (validate.isEmpty(this.state.updateModalServiceName)) {
      this.setState({
        updateModalError: "Service name cannot be empty",
      });
      return;
    }

    if (
      validate.single(this.state.updateModalServiceURL, {
        url: {
          allowLocal: true,
        },
      })
    ) {
      this.setState({
        updateModalError: "Service URL is invalid",
      });
      return;
    }

    const serviceToUpdate = {
      id: this.state.updateModalServiceID,
      name: this.state.updateModalServiceName,
      url: this.state.updateModalServiceURL,
    };

    updateService(serviceToUpdate)
      .then((res) => {
        console.log(res.data);
        this.toggleServiceUpdateModalView(false);
        this.updateServiceList();
      })
      .catch((error) => {
        console.log("Error", error.message);
      });
  }

  // delete service button click handler
  onServiceDelete() {
    deleteService(this.state.deleteModalServiceID)
      .then((res) => {
        console.log(res.data);
        this.toggleServiceDeleteModalView(false);
        this.updateServiceList();
      })
      .catch((error) => {
        console.log("Error", error.message);
      });
    this.toggleServiceDeleteModalView(false);
  }

  // add service dialog handler
  toggleServiceAddModalView(show) {
    this.setState({
      showServiceAddModal: show,
    });

    if (show === false) {
      this.setState({
        addModalServiceName: "",
        addModalServiceURL: "",
        addModalError: "",
      });
    }
  }

  // update service dialog handler
  toggleServiceUpdateModalView(show) {
    this.setState({
      showServiceUpdateModal: show,
    });

    if (show === false) {
      this.setState({
        updateModalServiceID: "",
        updateModalServiceName: "",
        updateModalError: "",
      });
    }
  }

  // delete service dialog handler
  toggleServiceDeleteModalView(show) {
    this.setState({
      showServiceDeleteModal: show,
    });

    if (show === false) {
      this.setState({
        deleteModalServiceID: "",
        deleteModalServiceName: "",
      });
    }
  }

  // render the add service dialog
  renderServiceAddModal() {
    return (
      <Modal
        show={this.state.showServiceAddModal}
        onHide={() => this.toggleServiceAddModalView(false)}
      >
        <Modal.Header closeButton>
          <Modal.Title>Add New Service</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {this.state.addModalError !== "" && (
            <Alert variant="danger">{this.state.addModalError}</Alert>
          )}
          <Form>
            <Form.Group className="mb-3" controlId="formName">
              <Form.Label>Name</Form.Label>
              <Form.Control
                type="text"
                value={this.state.addModalServiceName}
                onChange={(e) => {
                  this.setState({
                    addModalServiceName: e.target.value,
                    addModalError: "",
                  });
                }}
              />
            </Form.Group>
            <Form.Group className="mb-3" controlId="formURL">
              <Form.Label>URL</Form.Label>
              <Form.Control
                type="input"
                value={this.state.addModalServiceURL}
                onChange={(e) => {
                  this.setState({
                    addModalServiceURL: e.target.value,
                    addModalError: "",
                  });
                }}
              />
            </Form.Group>
          </Form>
        </Modal.Body>
        <Modal.Footer>
          <Button
            variant="secondary"
            onClick={() => this.toggleServiceAddModalView(false)}
          >
            Close
          </Button>
          <Button variant="primary" onClick={this.onServiceAdd}>
            Add
          </Button>
        </Modal.Footer>
      </Modal>
    );
  }

  // render the update service dialog
  renderServiceUpdateModal() {
    return (
      <Modal
        show={this.state.showServiceUpdateModal}
        onHide={() => this.toggleServiceUpdateModalView(false)}
      >
        <Modal.Header closeButton>
          <Modal.Title>Update Service</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {this.state.updateModalError !== "" && (
            <Alert variant="danger">{this.state.updateModalError}</Alert>
          )}
          <Form>
            <Form.Group className="mb-3" controlId="formName">
              <Form.Label>Name</Form.Label>
              <Form.Control
                type="text"
                value={this.state.updateModalServiceName}
                onChange={(e) => {
                  this.setState({
                    updateModalServiceName: e.target.value,
                    updateModalError: "",
                  });
                }}
              />
            </Form.Group>
            <Form.Group className="mb-3" controlId="formURL">
              <Form.Label>URL</Form.Label>
              <Form.Control
                type="input"
                value={this.state.updateModalServiceURL}
                onChange={(e) => {
                  this.setState({
                    updateModalServiceURL: e.target.value,
                    updateModalError: "",
                  });
                }}
              />
            </Form.Group>
          </Form>
        </Modal.Body>
        <Modal.Footer>
          <Button
            variant="secondary"
            onClick={() => this.toggleServiceUpdateModalView(false)}
          >
            Close
          </Button>
          <Button variant="primary" onClick={this.onServiceUpdate}>
            Update
          </Button>
        </Modal.Footer>
      </Modal>
    );
  }

  // render the delete service dialog
  renderServiceDeleteModal() {
    return (
      <Modal
        show={this.state.showServiceDeleteModal}
        onHide={() => this.toggleServiceDeleteModalView(false)}
      >
        <Modal.Header closeButton>
          <Modal.Title>Delete Service</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p>
            Are you sure you want to delete the service '
            {this.state.deleteModalServiceName}'
          </p>
        </Modal.Body>
        <Modal.Footer>
          <Button
            variant="secondary"
            onClick={() => this.toggleServiceDeleteModalView(false)}
          >
            Close
          </Button>
          <Button variant="primary" onClick={this.onServiceDelete}>
            Delete
          </Button>
        </Modal.Footer>
      </Modal>
    );
  }

  // render a service row
  renderServiceRow(svc) {
    return (
      <tr key={svc.id}>
        <td>{svc.name}</td>
        <td>{svc.url}</td>
        <td>{new Date(svc.createdDate).toString()}</td>
        <td style={{ textAlign: "center" }}>{svc.currentStatus}</td>
        <td style={{ textAlign: "center" }}>
          <Button
            variant="primary"
            onClick={() => {
              this.setState({
                updateModalServiceID: svc.id,
                updateModalServiceName: svc.name,
                updateModalServiceURL: svc.url,
              });
              this.toggleServiceUpdateModalView(true);
            }}
          >
            Update
          </Button>
        </td>
        <td style={{ textAlign: "center" }}>
          <Button
            variant="danger"
            onClick={() => {
              this.setState({
                deleteModalServiceID: svc.id,
                deleteModalServiceName: svc.name,
              });
              this.toggleServiceDeleteModalView(true);
            }}
          >
            Delete
          </Button>
        </td>
      </tr>
    );
  }

  render() {
    return (
      <Container>
        <Row>
          <Col>
            {this.renderServiceAddModal()}
            {this.renderServiceUpdateModal()}
            {this.renderServiceDeleteModal()}
            <div style={{ height: "40px" }}></div>
          </Col>
        </Row>
        <Row>
          <Col>
            <Button
              variant="primary"
              onClick={() => this.toggleServiceAddModalView(true)}
            >
              Add Service
            </Button>
          </Col>
        </Row>
        <Row>
          <Col>
            <div style={{ height: "10px" }}></div>
          </Col>
        </Row>
        <Row>
          <Col>
            <Table striped bordered hover>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>URL</th>
                  <th>Created Date</th>
                  <th>Status</th>
                  <th></th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {this.state.services.map((svc) => this.renderServiceRow(svc))}
              </tbody>
            </Table>
          </Col>
        </Row>
      </Container>
    );
  }
}

export default ServiceStatusPanel;
