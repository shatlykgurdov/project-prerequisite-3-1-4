document.addEventListener('DOMContentLoaded', () => {
    loadUsers();

    document.querySelector('#addUserForm').addEventListener('submit', createUser);
});

async function loadUsers() {
    const response = await fetch('/api/users');
    const users = await response.json();
    const usersTableBody = document.querySelector('#usersTable tbody');
    usersTableBody.innerHTML = '';

    users.forEach(user => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${user.id}</td>
            <td>${user.firstName}</td>
            <td>${user.lastName}</td>
            <td>${user.email}</td>
            <td>${user.role}</td>
            <td>
                <button class="btn btn-primary" onclick="editUser(${user.id})">Edit</button>
            </td>
            <td>
                <button class="btn btn-danger" onclick="deleteUser(${user.id})">Delete</button>
            </td>
        `;
        usersTableBody.appendChild(row);
    });
}

async function createUser(event) {
    event.preventDefault();
    const form = event.target;
    const formData = new FormData(form);
    const user = Object.fromEntries(formData);

    const response = await fetch('/api/users', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(user)
    });

    if (response.ok) {
        form.reset();
        loadUsers();
    } else {
        alert('Failed to create user');
    }
}

async function editUser(userId) {
    const response = await fetch(`/api/users/${userId}`);
    const user = await response.json();

    document.querySelector('#editUserId').value = user.id;
    document.querySelector('#editFirstName').value = user.firstName;
    document.querySelector('#editLastName').value = user.lastName;
    document.querySelector('#editEmail').value = user.email;
    document.querySelector('#editRole').value = user.role;

    new bootstrap.Modal(document.querySelector('#editUserModal')).show();
}

async function updateUser(event) {
    event.preventDefault();
    const form = event.target;
    const formData = new FormData(form);
    const user = Object.fromEntries(formData);
    const userId = document.querySelector('#editUserId').value;

    const response = await fetch(`/api/users/${userId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(user)
    });

    if (response.ok) {
        form.reset();
        new bootstrap.Modal(document.querySelector('#editUserModal')).hide();
        loadUsers();
    } else {
        alert('Failed to update user');
    }
}

async function deleteUser(userId) {
    const response = await fetch(`/api/users/${userId}`, {
        method: 'DELETE'
    });

    if (response.ok) {
        loadUsers();
    } else {
        alert('Failed to delete user');
    }
}
