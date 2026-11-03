import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import { ToastProvider } from './contexts/ToastContext'
import ProtectedRoute from './components/layout/ProtectedRoute'

import Landing from './pages/Landing'
import Login from './pages/Login'
import Register from './pages/Register'

import StudentDashboard from './pages/student/Dashboard'
import SubmitProject from './pages/student/SubmitProject'
import MyProjects from './pages/student/MyProjects'
import ProjectWorkflowDetails from './pages/student/ProjectWorkflowDetails'

import FacultyDashboard from './pages/faculty/Dashboard'
import ProjectWorkflow from './pages/faculty/ProjectWorkflow'

import AdminDashboard from './pages/admin/Dashboard'
import AdminRubrics from './pages/admin/Rubrics'

function App() {
  return (
    <Router>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            <Route path="/" element={<Landing />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            {/* Student Routes */}
            <Route path="/student/dashboard" element={
              <ProtectedRoute role="student"><StudentDashboard /></ProtectedRoute>
            } />
            <Route path="/student/submit" element={
              <ProtectedRoute role="student"><SubmitProject /></ProtectedRoute>
            } />
            <Route path="/student/projects" element={
              <ProtectedRoute role="student"><MyProjects /></ProtectedRoute>
            } />
            <Route path="/student/projects/:id" element={
              <ProtectedRoute role="student"><ProjectWorkflowDetails /></ProtectedRoute>
            } />

            {/* Faculty Routes */}
            <Route path="/faculty/dashboard" element={
              <ProtectedRoute role="faculty"><FacultyDashboard /></ProtectedRoute>
            } />
            <Route path="/faculty/projects/:id" element={
              <ProtectedRoute role="faculty"><ProjectWorkflow /></ProtectedRoute>
            } />

            {/* Admin Routes */}
            <Route path="/admin/dashboard" element={
              <ProtectedRoute role="admin"><AdminDashboard /></ProtectedRoute>
            } />
            <Route path="/admin/rubrics" element={
              <ProtectedRoute role="admin"><AdminRubrics /></ProtectedRoute>
            } />
            <Route path="/admin/projects/:id" element={
              <ProtectedRoute role="admin"><ProjectWorkflow /></ProtectedRoute>
            } />

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </Router>
  )
}

export default App
