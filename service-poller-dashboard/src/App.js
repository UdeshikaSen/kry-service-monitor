import React from "react";
import AdminNavbar from "./components/AdminNavbar";
import ServiceStatusPanel from "./components/ServiceStatusPanel";
import "./App.css";

class App extends React.Component {
  render() {
    return (
      <div className="wrapper">
        <div className="main-panel">
          <AdminNavbar />
          <div className="content">
            <ServiceStatusPanel />
          </div>
        </div>
      </div>
    );
  }
}
export default App;
