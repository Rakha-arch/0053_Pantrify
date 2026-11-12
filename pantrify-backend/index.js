const express = require('express');
const cors = require('cors');
require('dotenv').config();
const db = require('./config/db');

const app = express();
app.use(cors());
app.use(express.json());

app.get('/', async (req, res) => {
    try {
        await db.query('SELECT 1');
        res.json({
            status: 'success',
            message: 'Pantrify REST API & MySQL Database berjalan normal!'
        });
    } catch (error) {
        res.status(500).json({
            status: 'error',
            message: 'Gagal terhubung ke database',
            error: error.message
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server Pantrify berjalan di http://localhost:${PORT}`);
});
